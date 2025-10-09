#!/bin/bash
set -e

echo "Building payment-orchestrator module..."
mvn clean package -DskipTests -pl payment-orchestrator -am

echo "Build completed successfully!"
ls -lh payment-orchestrator/target/*.jar
#!/bin/bash
set -e

echo "Building api-gateway module..."
mvn clean package -DskipTests -pl api-gateway -am

echo "Build completed successfully!"
ls -lh api-gateway/target/*.jar

