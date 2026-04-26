# Stage 1: Build the application using Maven and Java 21
FROM maven:3.9.4-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .

# Use prod profile for compilation by default
ARG ENVIRONMENT=prod

# Skip running tests during the build to speed up the process
RUN mvn clean package -DskipTests -Dapp.environment=${ENVIRONMENT}

# Stage 2: Run the application using Java 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Create a user with configurable UID and GID
ENV PUID=1000
ENV PGID=1000
RUN groupadd -g ${PGID} appgroup && \
    useradd -u ${PUID} -g appgroup -m springuser
USER springuser

# Add health check
HEALTHCHECK --interval=30s --timeout=3s CMD wget -q --spider http://localhost:8181/actuator/health || exit 1

# Expose the application port
EXPOSE 8181

# Set Spring profile to prod by default
ARG ENVIRONMENT=prod
ENV SPRING_PROFILES_ACTIVE=${ENVIRONMENT}

# Add JVM options for container environments
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
