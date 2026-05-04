# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk-alpine AS dev
WORKDIR /app
RUN apk add --no-cache curl
EXPOSE 8084
CMD ["sh", "-c", "cd / && ./gradlew :apps:caq-compliance-svc:bootRun --no-daemon"]

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY ../../gradle ./gradle
COPY ../../gradlew ./gradlew
COPY ../../settings.gradle.kts ../../build.gradle.kts ../../gradle.properties ./
COPY . ./apps/caq-compliance-svc
RUN chmod +x gradlew && ./gradlew :apps:caq-compliance-svc:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine AS prod
WORKDIR /app
RUN apk add --no-cache curl tini && addgroup -S caqi -g 1000 && adduser -S caqi -G caqi -u 1000
COPY --from=builder /build/apps/caq-compliance-svc/build/libs/caq-compliance-svc.jar /app/app.jar
USER caqi
EXPOSE 8084
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl --fail http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["/sbin/tini", "--", "java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
