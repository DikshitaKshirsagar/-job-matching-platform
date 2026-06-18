#!/bin/sh
# Docker entrypoint script for production
# Reads Docker secrets files and exports them as environment variables
# Spring Boot reads standard env vars (not _FILE suffixed ones)

set -e

# Function to read a secret file and export its content as an environment variable
# Usage: read_secret VAR_NAME SECRET_FILE
# Example: read_secret DB_PASSWORD /run/secrets/db_password
read_secret() {
  var_name="$1"
  secret_file="$2"
  if [ -f "$secret_file" ]; then
    value=$(cat "$secret_file" | tr -d '\n\r')
    export "$var_name"="$value"
    echo "[ENTRYPOINT] Loaded $var_name from secret file"
  else
    echo "[ENTRYPOINT] WARNING: Secret file $secret_file not found for $var_name"
  fi
}

# Read all secrets
read_secret DB_PASSWORD /run/secrets/db_password
read_secret JWT_SECRET /run/secrets/jwt_secret
read_secret AI_SERVICE_API_KEY /run/secrets/ai_service_api_key

# Execute the main command (Spring Boot jar)
echo "[ENTRYPOINT] Starting Spring Boot application..."
exec java -jar /app/app.jar