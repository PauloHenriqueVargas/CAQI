plugins {
    alias(libs.plugins.spring.boot)
}

description = "Gestão escolar/operacional: escolas, turmas, alunos, matrículas, censo, PNAE/PNATE, pessoal/folha."

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.jackson.dataformat.csv)
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
    archiveFileName.set("caq-escolar-svc.jar")
}
