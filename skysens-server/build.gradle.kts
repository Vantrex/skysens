// ---------------------------------------------------------------------------
// skysens-server — plain Spring Boot HTTP service. NOT a Minecraft server.
// Neither Minecraft nor Fabric may ever appear on this classpath.
//
// Spring Boot 4.1.1 supports Java 17..26 inclusive, which covers the JDK 25
// toolchain set by the root script.
// Source: https://docs.spring.io/spring-boot/system-requirements.html
//
// Deliberately minimal: web + validation only. There is no persistence layer yet,
// so there are no JPA, Flyway or JDBC driver dependencies. Add them in the same
// commit as the first entity that needs them, not before.
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

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
