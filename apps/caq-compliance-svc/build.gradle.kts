plugins {
    alias(libs.plugins.spring.boot)
}

description = "Validações legais (MDE/Fundeb/VAAR), auditoria imutável (chain SHA-256), ROPA/LGPD, transparência LAI, integração SIOPE."

dependencies {
    implementation(project(":platform:caq-shared-domain"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.amqp)
    runtimeOnly(libs.postgresql)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.spring.cloud.starter.circuitbreaker.resilience4j)
    implementation(libs.micrometer.tracing.bridge.otel)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.opentelemetry.exporter.otlp)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("caq-compliance-svc.jar")
}
