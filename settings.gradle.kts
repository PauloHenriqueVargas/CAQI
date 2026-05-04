rootProject.name = "caqi"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

include(
    "platform:caq-shared-domain",
    "apps:caq-engine-svc",
    "apps:caq-financeiro-svc",
    "apps:caq-escolar-svc",
    "apps:caq-compliance-svc",
)

// Renomeia projetos para nomes simples (sem o prefixo "apps:")
rootProject.children.forEach { p ->
    p.children.forEach { sub ->
        sub.name = sub.name
    }
}
