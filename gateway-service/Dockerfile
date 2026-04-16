# Gateway_Service Dockerfile
# Multi-stage build: Maven build + Java 25 runtime

# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy POM first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# --- Stage 2: Runtime ---
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the built artifact
COPY --from=build /app/target/quarkus-app /app/quarkus-app

EXPOSE 8080

ENV JAVA_OPTS=""

CMD ["java", "-jar", "/app/quarkus-app/quarkus-run.jar"]
