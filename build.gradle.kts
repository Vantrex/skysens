plugins {
    id("net.fabricmc.fabric-loom") version "1.17.12"
    id("maven-publish")
    id("io.freefair.lombok") version "9.1.0"
    id("com.gradleup.shadow") version "9.3.0"
}

repositories {
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://maven.notenoughupdates.org/releases/")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

val mod_version: String = (project.findProperty("mod_version") as? String) ?: "1.0-SNAPSHOT"
val maven_group: String = (project.findProperty("maven_group") as? String) ?: "de.vantrex"
val archives_base_name: String = (project.findProperty("archives_base_name") as? String) ?: "skysens"
val minecraft_version: String = (project.findProperty("minecraft_version") as? String) ?: "26.1.2"
val loader_version: String = (project.findProperty("loader_version") as? String) ?: "0.19.3"
val fabric_version: String = (project.findProperty("fabric_version") as? String) ?: "0.153.0+26.1.2"
val hypixel_mod_api_version: String = (project.findProperty("hypixel_mod_api_version") as? String) ?: "1.0.2"
val moulconfig_version: String = (project.findProperty("moulconfig_version") as? String) ?: "4.7.2"
val fabric_language_kotlin_version: String = (project.findProperty("fabric_language_kotlin_version") as? String) ?: "1.13.12+kotlin.2.4.0"

version = mod_version
group = maven_group

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
    from("LICENSE.txt") { rename { "${it}_${archives_base_name}" } }
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
    from("LICENSE.txt") { rename { "${it}_${archives_base_name}" } }
}

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
