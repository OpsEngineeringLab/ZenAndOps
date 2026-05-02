# Dashboard_Service Dockerfile
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

# Compile a tiny health-check class (no external deps)
RUN printf 'import java.net.*;\npublic class HealthCheck {\n  public static void main(String[] a) throws Exception {\n    var c=(HttpURLConnection)URI.create(a[0]).toURL().openConnection();\n    c.setConnectTimeout(3000);c.setReadTimeout(3000);\n    System.exit(c.getResponseCode()==200?0:1);\n  }\n}\n' > /tmp/HealthCheck.java && javac /tmp/HealthCheck.java -d /tmp/healthcheck

# --- Stage 2: Runtime ---
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy health check utility
COPY --from=build /tmp/healthcheck/HealthCheck.class /app/healthcheck/

# Copy the built artifact
COPY --from=build /app/target/quarkus-app /app/quarkus-app

EXPOSE 8082

ENV JAVA_OPTS=""

CMD ["java", "-jar", "/app/quarkus-app/quarkus-run.jar"]
