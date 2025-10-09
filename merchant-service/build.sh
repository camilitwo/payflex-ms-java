#!/bin/bash
set -e

echo "Building merchant-service module..."
mvn clean package -DskipTests -pl merchant-service -am

echo "Build completed successfully!"
ls -lh merchant-service/target/*.jar

