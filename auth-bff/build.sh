#!/bin/bash
set -e

echo "Building auth-bff module..."
mvn clean package -DskipTests -pl auth-bff -am

echo "Build completed successfully!"
ls -lh auth-bff/target/*.jar

