plugins {
    id("fabric-loom") version "1.14-SNAPSHOT"
    id("maven-publish")
    id("io.freefair.lombok") version "9.1.0"
    id("com.gradleup.shadow") version "9.3.0"
}

val mod_version: String by project
val maven_group: String by project
val archives_base_name: String by project
val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val fabric_version: String by project

version = mod_version
group = maven_group

base {
    archivesName.set(archives_base_name)
}

val shadowModImpl by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("skysens") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
    }
}

fabricApi {
    configureDataGeneration {
        client.set(true)
    }
}

repositories {
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://maven.notenoughupdates.org/releases/")
}

dependencies {
    // To change the versions see the gradle.properties file
    "minecraft"("com.mojang:minecraft:$minecraft_version")
    "mappings"("net.fabricmc:yarn:$yarn_mappings:v2")
    "modImplementation"("net.fabricmc:fabric-loader:$loader_version")

    "modImplementation"("net.fabricmc.fabric-api:fabric-api:$fabric_version")
    "shadowModImpl"("org.notenoughupdates.moulconfig:modern-1.21.10:4.2.0-beta")
    implementation("net.hypixel:mod-api:1.0.1")
}

tasks.processResources {
    inputs.property("version", mod_version)
    inputs.property("minecraft_version", minecraft_version)
    inputs.property("loader_version", loader_version)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to mod_version,
            "minecraft_version" to minecraft_version,
            "loader_version" to loader_version
        )
    }
}

val targetJavaVersion = 21
tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        options.release.set(targetJavaVersion)
    }
}

tasks.shadowJar {
    // Make sure to relocate MoulConfig to avoid version clashes with other mods
    configurations = listOf(shadowModImpl)
    relocate("io.github.notenoughupdates.moulconfig", "my.mod.deps.moulconfig")
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${archives_base_name}" }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = archives_base_name
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
