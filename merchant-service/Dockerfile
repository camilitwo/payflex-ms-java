# Multi-stage build para optimizar el tamaño de la imagen
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /build

# Copiar el pom.xml del padre
COPY ../pom.xml ./pom.xml

# Copiar los poms de todos los módulos para aprovechar cache de dependencias
COPY ../merchant-service/pom.xml ./merchant-service/pom.xml
COPY ../auth-bff/pom.xml ./auth-bff/pom.xml
COPY ../merchant-service/pom.xml ./merchant-service/pom.xml
COPY ../payment-orchestrator/pom.xml ./payment-orchestrator/pom.xml

# Descargar dependencias (se cachea esta capa)
RUN mvn dependency:go-offline -B -pl api-gateway -am || true

# Copiar el código fuente solo del api-gateway
COPY ../merchant-service/src ./merchant-service/src

# Construir solo el módulo api-gateway
RUN mvn clean package -pl api-gateway -am -DskipTests

# Etapa final con JRE mínimo
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Crear usuario no-root para mayor seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el JAR desde la etapa de build
COPY --from=build /build/merchant-service/target/*.jar app.jar

# Railway asigna automáticamente PORT
EXPOSE 8080

# Configuración JVM optimizada para Railway
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ESTE DOCKERFILE NO SE USA
# Railway debe usar el archivo Dockerfile.api-gateway en la raíz del proyecto
# Ver RAILWAY_DEPLOY.md para instrucciones
