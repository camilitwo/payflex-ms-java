# Dockerfile autocontenido para build desde /auth-bff
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

# 1) Instalar el POM padre en el repo local para resolver la herencia del módulo
#    Esto evita depender del contexto del repo completo cuando Railway limita el Root Directory
RUN mkdir -p /tmp && cat > /tmp/payflex-parent.pom <<'EOF'
<project xmlns="http://maven.apache.org/POM/4.0.0"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0  https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.payflex</groupId>
  <artifactId>payflex-mvp</artifactId>
  <version>1.0.0</version>
  <packaging>pom</packaging>
  <properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <spring.boot.version>3.3.3</spring.boot.version>
    <spring.cloud.version>2023.0.3</spring.cloud.version>
  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring.boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>${spring.cloud.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-maven-plugin</artifactId>
          <version>${spring.boot.version}</version>
          <executions>
            <execution>
              <goals>
                <goal>repackage</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.11.0</version>
          <configuration>
            <source>21</source>
            <target>21</target>
            <encoding>UTF-8</encoding>
          </configuration>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
EOF
RUN mvn -B -N -f /tmp/payflex-parent.pom install

# 2) Copiar el módulo y compilar
COPY pom.xml ./pom.xml
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests clean package
# Guardar el JAR para copiar fácil a la etapa final
RUN JAR_FILE=$(find target -type f -name "*.jar" ! -name "*original*" | head -n1) && cp "$JAR_FILE" /tmp/app.jar

# 3) Etapa de runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Usuario no root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /tmp/app.jar /app/app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh","-c","exec java -Dserver.port=${PORT:-8080} $JAVA_OPTS -jar /app/app.jar"]

