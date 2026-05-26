import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.spring") version "2.0.20"
    kotlin("plugin.jpa") version "2.0.20"
}

group = "de.healthforge"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Persistence
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:10.18.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.18.0")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Rate Limiting
    implementation("com.bucket4j:bucket4j-core:8.10.1")

    // OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // MinIO
    implementation("io.minio:minio:8.5.12")

    // Image processing
    implementation("net.coobird:thumbnailator:0.4.20")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // BCrypt is part of spring-security-crypto (transitive via starter-security)

    // Dev tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // ===== Smoke-Tests (Auth) =====
    // LOCKED Q10 reopen 2026-05-25: Smoke-Test-Layer for Auth flow (register/login/refresh/logout)
    // via Testcontainers-PG. Other modules remain test-free for v1.0.
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito") // not used; keeps classpath smaller
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter:1.20.2")
    testImplementation("org.testcontainers:postgresql:1.20.2")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // LOCKED Q10 reopen: Smoke tests enabled (only AuthIT). Use `gradle test` to run.
}
