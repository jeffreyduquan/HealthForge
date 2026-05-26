# Multi-stage build for HealthForge server
FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /workspace

# Copy gradle config first for layer caching
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN gradle --no-daemon dependencies > /dev/null 2>&1 || true

# Copy source and build
COPY src ./src
RUN gradle --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
