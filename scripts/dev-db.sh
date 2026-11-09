#!/usr/bin/env bash
# Start a PostgreSQL instance for local development without Docker Compose.
# Usage: scripts/dev-db.sh [container-name]
set -euo pipefail

NAME="${1:-examprep-dev-db}"
PORT="${POSTGRES_PORT:-5432}"
DB="${POSTGRES_DB:-examprep}"
USER="${SPRING_DATASOURCE_USERNAME:-examprep}"
PASSWORD="${SPRING_DATASOURCE_PASSWORD:-change-me}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required for this helper. Install PostgreSQL locally instead and set SPRING_DATASOURCE_URL." >&2
  exit 1
fi

if docker ps -a --format '{{.Names}}' | grep -qx "$NAME"; then
  echo "Starting existing container $NAME"
  docker start "$NAME" >/dev/null
else
  echo "Creating container $NAME on port $PORT"
  docker run -d --name "$NAME" \
    -e POSTGRES_DB="$DB" \
    -e POSTGRES_USER="$USER" \
    -e POSTGRES_PASSWORD="$PASSWORD" \
    -p "${PORT}:5432" \
    -v "${NAME}-data:/var/lib/postgresql/data" \
    postgres:16-alpine >/dev/null
fi

echo "Waiting for PostgreSQL to accept connections..."
for _ in $(seq 1 30); do
  if docker exec "$NAME" pg_isready -U "$USER" -d "$DB" >/dev/null 2>&1; then
    echo "PostgreSQL ready: jdbc:postgresql://localhost:${PORT}/${DB} (user ${USER})"
    exit 0
  fi
  sleep 1
done

echo "PostgreSQL did not become ready in time. Check: docker logs $NAME" >&2
exit 1
