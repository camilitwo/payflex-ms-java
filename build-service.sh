#!/bin/bash
set -e

# Script de build inteligente para Railway
# Lee la variable SERVICE_NAME para saber qué módulo compilar

if [ -z "$SERVICE_NAME" ]; then
    echo "❌ ERROR: Variable SERVICE_NAME no está definida"
    echo "Define SERVICE_NAME con uno de: api-gateway, auth-bff, merchant-service, payment-orchestrator"
    exit 1
fi

echo "🔨 Building module: $SERVICE_NAME"
echo "================================"

# Compilar el módulo específico
mvn clean package -DskipTests -pl "$SERVICE_NAME" -am

# Verificar que el JAR fue creado
JAR_PATH="$SERVICE_NAME/target/$SERVICE_NAME-1.0.0.jar"

if [ -f "$JAR_PATH" ]; then
    echo "✅ Build successful: $JAR_PATH"
    ls -lh "$JAR_PATH"
else
    echo "❌ ERROR: JAR not found at $JAR_PATH"
    echo "Contents of $SERVICE_NAME/target/:"
    ls -lh "$SERVICE_NAME/target/" || echo "Directory not found"
    exit 1
fi

echo "================================"
echo "✅ Build completed successfully"

