# syntax=docker/dockerfile:1.7

# ---- builder ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY ../../gradle ./gradle
COPY ../../gradlew ./gradlew
COPY ../../settings.gradle.kts ../../build.gradle.kts ../../gradle.properties ./
COPY . ./apps/caq-engine-svc
RUN chmod +x gradlew && ./gradlew :apps:caq-engine-svc:bootJar --no-daemon

# ---- dev (compose monta volume com código + roda Gradle dentro) ----
FROM eclipse-temurin:21-jdk-alpine AS dev
WORKDIR /app
RUN apk add --no-cache curl
EXPOSE 8081
CMD ["sh", "-c", "cd / && ./gradlew :apps:caq-engine-svc:bootRun --no-daemon"]

# ---- prod ----
FROM eclipse-temurin:21-jre-alpine AS prod
WORKDIR /app
RUN apk add --no-cache curl tini \
 && addgroup -S caqi -g 1000 \
 && adduser -S caqi -G caqi -u 1000
COPY --from=builder /build/apps/caq-engine-svc/build/libs/caq-engine-svc.jar /app/app.jar
USER caqi
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl --fail http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["/sbin/tini", "--", "java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
