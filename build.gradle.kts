plugins {
    // For 1.21.10 (obfuscated), Fabric recommends the remap plugin id (legacy "fabric-loom" still works)
   // id("net.fabricmc.fabric-loom-remap") version "1.14-SNAPSHOT"
    id("fabric-loom") version "1.14-SNAPSHOT"
    id("maven-publish")
    id("io.freefair.lombok") version "9.1.0"
    id("com.gradleup.shadow") version "9.3.0"
}

repositories {
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://maven.notenoughupdates.org/releases/")
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

// put ONLY the deps you want shaded into these:
val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}
val shadowModImpl: Configuration by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("skysens") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])

            // recommended if you shade deps (dev env grouping)
            configuration(shadowModImpl)
            configuration(shadowImpl)
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings("net.fabricmc:yarn:$yarn_mappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    // ✅ shade + relocate MoulConfig (recommended by MoulConfig docs)
    shadowModImpl("org.notenoughupdates.moulconfig:modern-1.21.10:4.2.0-beta")

    // ✅ provide Kotlin runtime via Fabric Language Kotlin (DON’T relocate kotlin.*)
    // pick one:
    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.8+kotlin.2.3.0")
    // (1.13.6+kotlin.2.2.20 is also a common pick for 1.21.10)
    // modImplementation("net.fabricmc:fabric-language-kotlin:1.13.6+kotlin.2.2.20")

    // Hypixel Mod API:
    // If you want it as an external required mod, keep it as modImplementation.
    modImplementation("net.hypixel:mod-api:1.0.1")

    // If instead you want it INSIDE your jar, move it to shadowModImpl(...) — but only do this
    // if you're sure the artifact is a "library jar" and not a full mod jar with its own fabric.mod.json.
    // shadowModImpl("net.hypixel:mod-api:1.0.1")
}

tasks.jar {
    archiveClassifier.set("dev")
    from("LICENSE.txt") { rename { "${it}_${archives_base_name}" } }
}


tasks.shadowJar {
    mergeServiceFiles()
    configurations = listOf(shadowModImpl, shadowImpl)

    // ✅ relocate MoulConfig package
    relocate(
        "io.github.notenoughupdates.moulconfig",
        "de.vantrex.skysens.dependencies.moulconfig"
    )

    // ❌ do NOT relocate kotlin.*
    // relocate("kotlin", "...") <-- remove

    from(sourceSets.main.get().output)
    from(sourceSets["client"].output)
    from("LICENSE.txt") { rename { "${it}_${archives_base_name}" } }

    archiveClassifier.set("dev-shadow")
}


/*
tasks.shadowJar {
    // ✅ REQUIRED for ServiceLoader (MoulConfig uses it)
    mergeServiceFiles()

    // If Gradle complains about duplicate entries, enable this:
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    configurations = listOf(shadowModImpl, shadowImpl)
    relocate("io.github.notenoughupdates.moulconfig", "de.vantrex.skysens.dependencies.moulconfig")
    // (and I'd still recommend NOT relocating kotlin.*, but that's separate)
}
 */
tasks.remapJar {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    archiveClassifier.set("") // this becomes your distributable jar
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString(),
        "minecraft_version" to (project.findProperty("minecraft_version") as String),
        "loader_version" to (project.findProperty("loader_version") as String),
    )

    inputs.properties(props)

    filesMatching("fabric.mod.json") {
        expand(props)
    }
}