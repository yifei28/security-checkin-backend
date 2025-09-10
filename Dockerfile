# Multi-stage build for optimized image
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Copy all source files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# One-step build with optimizations and timeouts
RUN chmod +x mvnw && \
    ./mvnw package -DskipTests \
    --batch-mode \
    --no-transfer-progress \
    --threads 1C \
    -Dmaven.wagon.http.connectionTimeout=60000 \
    -Dmaven.wagon.http.readTimeout=60000 \
    -Dmaven.wagon.rto=60000

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Add non-root user for security
RUN groupadd --gid 1001 spring && \
    useradd --uid 1001 --gid spring --shell /bin/bash --create-home spring

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Copy JAR from builder
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

USER spring

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# JVM options will be passed via environment variables
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]