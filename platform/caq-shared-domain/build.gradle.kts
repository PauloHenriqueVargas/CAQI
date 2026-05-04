plugins {
    `java-library`
}

description = "DTOs e eventos compartilhados entre os microserviços CAQ. Sem dependências de framework — apenas stdlib."

// Não aplica spring-boot — é uma biblioteca pura.
// Os subprojects do root build.gradle.kts ainda aplicam io.spring.dependency-management
// (não causa problema, mas não traz BOM Spring para cá).
