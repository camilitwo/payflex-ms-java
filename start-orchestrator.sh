#!/bin/bash

echo "🚀 Iniciando Payment Orchestrator..."

cd "$(dirname "$0")/payment-orchestrator"

# Verificar si hay Maven Wrapper
if [ -f "../mvnw" ]; then
    echo "📦 Compilando con Maven Wrapper..."
    ../mvnw clean package -DskipTests
elif command -v mvn &> /dev/null; then
    echo "📦 Compilando con Maven..."
    mvn clean package -DskipTests
else
    echo "⚠️  Maven no encontrado. Intentando ejecutar el JAR existente..."
fi

# Ejecutar el JAR
if [ -f "target/payment-orchestrator-1.0.0.jar" ]; then
    echo "▶️  Ejecutando Payment Orchestrator en puerto 8082..."
    java -jar target/payment-orchestrator-1.0.0.jar
else
    echo "❌ Error: No se encontró el JAR compilado en target/"
    echo "   Por favor, compila el proyecto primero con Maven o IntelliJ IDEA"
    exit 1
fi

