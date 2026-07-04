# Dockerfile pour GraalVM Native Image - Exécutable Pré-compilé
# L'exécutable natif est compilé en local et copié dans ce dossier
# Build Render: <30s | Démarrage: <2s | Mémoire: 50-100MB

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
COPY backend ./backend

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
