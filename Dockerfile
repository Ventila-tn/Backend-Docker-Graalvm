# Dockerfile pour Render - Exécutable Native Pré-compilé
# 
# IMPORTANT: Vous devez d'abord compiler l'exécutable natif en local:
#   1. ./build-native.sh (Linux/macOS) OU build-native.bat (Windows)
#   2. Copier backend/target/backend vers Backend-Docker-Graalvm/backend
#   3. Deployer sur Render
#
# Démarrage: <2s | Mémoire: 50-100MB | Build Render: <30s

FROM debian:bookworm-slim

WORKDIR /app

# Install only essential runtime libraries
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    ca-certificates \
    libssl3 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy the pre-built native executable
# IMPORTANT: L'exécutable doit être présent dans le repo
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
