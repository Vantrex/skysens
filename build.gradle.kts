// ---------------------------------------------------------------------------
// Root build script — CONVENTIONS ONLY. No code, no source sets, no Loom.
//
// Hard constraint 1: `fabric-loom`, `splitEnvironmentSourceSets()`, the
// `loom { mods { ... } }` block and `shadowJar` are declared ONLY in
// skysens-mod/build.gradle.kts. Applying Loom here would make Gradle try to
// provide and remap Minecraft for skysens-common and skysens-server too, which
// breaks both of them.
// ---------------------------------------------------------------------------

plugins {
    // Applied to every subproject below; never applied to the root project itself.
    id("io.freefair.lombok") apply false
}

// gradle.properties remains the single source of version truth (constraint 7).
val modVersion: String = (project.findProperty("mod_version") as? String) ?: "1.0-SNAPSHOT"
val mavenGroup: String = (project.findProperty("maven_group") as? String) ?: "de.vantrex"

allprojects {
    group = mavenGroup
    version = modVersion
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.freefair.lombok")

    repositories {
        mavenCentral()
        maven("https://repo.hypixel.net/repository/Hypixel/")
        maven("https://maven.notenoughupdates.org/releases/")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
