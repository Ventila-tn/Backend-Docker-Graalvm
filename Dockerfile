# Multi-stage Dockerfile pour GraalVM Native Image
# Build l'exécutable natif directement dans Docker
# Démarrage: <2s | Mémoire: 50-100MB | Build: 5-10 minutes

# Stage 1: Build avec GraalVM
FROM ghcr.io/graalvm/native-image-community:21 AS builder

# Install Maven prerequisites
RUN microdnf install -y findutils

WORKDIR /build

# Copy Maven wrapper and pom.xml first for better caching
COPY backend/mvnw backend/mvnw.cmd backend/pom.xml ./
COPY backend/.mvn ./.mvn

# Ensure mvnw is executable
RUN chmod +x ./mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY backend/src ./src

# Build native image
# This takes 5-10 minutes but produces a tiny, fast executable
RUN ./mvnw -Pnative native:compile -DskipTests

# Stage 2: Create minimal runtime image
FROM debian:bookworm-slim

WORKDIR /app

# Install essential runtime libraries + curl (needed for HEALTHCHECK)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    ca-certificates \
    libssl3 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy the native executable from builder
COPY --from=builder /build/target/backend ./backend

# Make executable
RUN chmod +x ./backend

# Create non-root user for security
RUN useradd -r -u 1001 -g root appuser && \
    chown -R appuser:root /app

USER appuser

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the native executable
ENTRYPOINT ["./backend"]
