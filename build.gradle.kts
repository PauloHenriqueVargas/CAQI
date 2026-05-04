plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

allprojects {
    group = "br.com.caqi"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    extensions.configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.spring.boot.get()}")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${rootProject.libs.versions.spring.cloud.get()}")
            mavenBom("io.opentelemetry:opentelemetry-bom:${rootProject.libs.versions.opentelemetry.get()}")
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Werror"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        systemProperty("file.encoding", "UTF-8")
        testLogging {
            events("passed", "failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

// Atalho para subir o ambiente local (referencia o docker-compose da raiz).
tasks.register<Exec>("composeUp") {
    group = "caqi"
    description = "Sobe a infra local (postgres, redis, rabbitmq) via docker-compose."
    commandLine("docker", "compose", "up", "-d", "postgres", "redis", "rabbitmq")
}

tasks.register<Exec>("composeDown") {
    group = "caqi"
    description = "Derruba toda a infra local."
    commandLine("docker", "compose", "down")
}

// Helper para acessar o version catalog em subprojects (workaround Gradle).
val Project.libs get() = the<org.gradle.accessors.dm.LibrariesForLibs>()
