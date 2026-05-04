plugins {
    alias(libs.plugins.spring.boot)
}

description = "Motor CAQ/CAQi — cálculo, simulações, parâmetros, índices, insumos. Owner do schema."

dependencies {
    implementation(project(":platform:caq-shared-domain"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.amqp)

    // Owner das migrations Flyway (único serviço com flyway-enabled).
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)

    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.spring.cloud.starter.circuitbreaker.resilience4j)

    // Observability
    implementation(libs.micrometer.tracing.bridge.otel)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.opentelemetry.exporter.otlp)

    // Lombok (compile-time)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // MapStruct
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    // Tests
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("caq-engine-svc.jar")
}
