#!/bin/bash
set -e

# Script de inicio inteligente para Railway
# Lee la variable SERVICE_NAME para saber qué JAR ejecutar

if [ -z "$SERVICE_NAME" ]; then
    echo "❌ ERROR: Variable SERVICE_NAME no está definida"
    exit 1
fi

JAR_PATH="$SERVICE_NAME/target/$SERVICE_NAME-1.0.0.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "❌ ERROR: JAR not found at $JAR_PATH"
    exit 1
fi

echo "🚀 Starting $SERVICE_NAME..."
echo "JAR: $JAR_PATH"
echo "PORT: $PORT"
echo "================================"

exec java -Dserver.port=$PORT $JAVA_TOOL_OPTIONS -jar "$JAR_PATH"

