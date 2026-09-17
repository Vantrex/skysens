// ---------------------------------------------------------------------------
// skysens-server — plain Spring Boot HTTP service. NOT a Minecraft server.
// Neither Minecraft nor Fabric may ever appear on this classpath.
//
// Spring Boot 4.1.1 supports Java 17..26 inclusive, which covers the JDK 25
// toolchain set by the root script.
// Source: https://docs.spring.io/spring-boot/system-requirements.html
// ---------------------------------------------------------------------------

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("io.freefair.lombok")
}

dependencies {
    // Dependency direction is one-way: server -> common. Never the reverse.
    // A plain `implementation` is correct here: bootJar packages the full runtime
    // classpath, so no shadow trickery is needed on this side.
    implementation(project(":skysens-common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql")

    // `dev` profile runs against in-memory H2 so the skeleton boots with no
    // external service. See application.yml.
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
