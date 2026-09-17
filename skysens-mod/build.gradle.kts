// ---------------------------------------------------------------------------
// skysens-mod — the Fabric mod. This is the ONLY module that applies Loom.
// Everything in this file came from the old root build.gradle.kts unchanged
// except where noted.
// ---------------------------------------------------------------------------

plugins {
    id("net.fabricmc.fabric-loom")
    id("maven-publish")
    id("io.freefair.lombok")
    id("com.gradleup.shadow")
}

// gradle.properties stays the single source of version truth (constraint 7).
val mod_version: String = (project.findProperty("mod_version") as? String) ?: "1.0-SNAPSHOT"
val archives_base_name: String = (project.findProperty("archives_base_name") as? String) ?: "skysens"
val minecraft_version: String = (project.findProperty("minecraft_version") as? String) ?: "26.1.2"
val loader_version: String = (project.findProperty("loader_version") as? String) ?: "0.19.3"
val fabric_version: String = (project.findProperty("fabric_version") as? String) ?: "0.153.0+26.1.2"
val hypixel_mod_api_version: String = (project.findProperty("hypixel_mod_api_version") as? String) ?: "1.0.2"
val moulconfig_version: String = (project.findProperty("moulconfig_version") as? String) ?: "4.7.2"
val fabric_language_kotlin_version: String = (project.findProperty("fabric_language_kotlin_version") as? String) ?: "1.13.12+kotlin.2.4.0"

base {
    archivesName.set(archives_base_name)
}

// Shadow configurations
val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}
val shadowModImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("skysens") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.named("client").get())
            configuration(shadowModImpl)
            configuration(shadowImpl)
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    implementation("net.fabricmc:fabric-loader:$loader_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    // skysens-common — the project's own code, and it MUST end up inside the mod
    // jar. `tasks.shadowJar` sets `configurations = listOf(shadowModImpl, shadowImpl)`,
    // so a plain `implementation(project(...))` would never be bundled and the mod
    // would NoClassDefFoundError at runtime (constraint 5 carve-out, §4.2 option (a)).
    //
    // Option (a) was chosen over (b) `from(project(":skysens-common").sourceSets...)`
    // because (b) reaches across project boundaries at configuration time, which is
    // hostile to Gradle 9 / the configuration cache and sets up no task dependency,
    // so the jar can be built from stale or missing output. (a) is an ordinary
    // dependency: Gradle wires the task graph itself. Note the `client` source set
    // inherits `implementation`, exactly as it already does for MoulConfig.
    shadowImpl(project(":skysens-common"))

    // MoulConfig (shaded)
    shadowModImpl("org.notenoughupdates.moulconfig:modern-26.1:$moulconfig_version") {
        exclude(module = "fabric-api")
        exclude(module = "minecraft")
    }

    // Fabric Language Kotlin
    implementation("net.fabricmc:fabric-language-kotlin:$fabric_language_kotlin_version")

    // Hypixel Mod API
    implementation("net.hypixel:mod-api:$hypixel_mod_api_version")
}

tasks.shadowJar {
    mergeServiceFiles()
    configurations = listOf(shadowModImpl, shadowImpl)
    relocate("io.github.notenoughupdates.moulconfig", "de.vantrex.skysens.dependencies.moulconfig")
    from(sourceSets.main.get().output)
    from(sourceSets.named("client").get().output)
    from(rootProject.file("LICENSE.txt")) { rename { "${it}_${archives_base_name}" } }
    archiveClassifier.set("")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to archives_base_name,
            "Implementation-Version" to project.version
        )
    }
    archiveClassifier.set("dev")
    from(rootProject.file("LICENSE.txt")) { rename { "${it}_${archives_base_name}" } }
}

// Must stay with the mod: without it the shipped fabric.mod.json contains a
// literal ${version}. Still reads from gradle.properties.
tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString(),
        "minecraft_version" to minecraft_version,
        "loader_version" to loader_version,
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}
