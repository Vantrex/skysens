plugins {
    id("net.fabricmc.fabric-loom") version "1.18.4"
    id("maven-publish")
    id("io.freefair.lombok") version "9.1.0"
    id("com.gradleup.shadow") version "9.3.0"
    id("com.modrinth.minotaur") version "2.8.7"
}

repositories {
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://maven.notenoughupdates.org/releases/")
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

val mod_version: String = (project.findProperty("mod_version") as? String) ?: "1.0-SNAPSHOT"
val maven_group: String = (project.findProperty("maven_group") as? String) ?: "de.vantrex"
val archives_base_name: String = (project.findProperty("archives_base_name") as? String) ?: "skysens"
val minecraft_version: String = (project.findProperty("minecraft_version") as? String) ?: "26.2"
val loader_version: String = (project.findProperty("loader_version") as? String) ?: "0.19.5"
val fabric_version: String = (project.findProperty("fabric_version") as? String) ?: "0.161.0+26.2"
val hypixel_mod_api_version: String = (project.findProperty("hypixel_mod_api_version") as? String) ?: "1.0.2"
val hypixel_mod_api_fabric_version: String = (project.findProperty("hypixel_mod_api_fabric_version") as? String) ?: "1.0.2+build.1+mc26.1"
val moulconfig_version: String = (project.findProperty("moulconfig_version") as? String) ?: "4.7.2"
val fabric_language_kotlin_version: String = (project.findProperty("fabric_language_kotlin_version") as? String) ?: "1.14.2+kotlin.2.4.21"

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
    shadowModImpl("org.notenoughupdates.moulconfig:modern-26.2:$moulconfig_version") {
        exclude(module = "fabric-api")
        exclude(module = "minecraft")
    }

    // Fabric Language Kotlin
    implementation("net.fabricmc:fabric-language-kotlin:$fabric_language_kotlin_version")

    // Hypixel Mod API
    implementation("net.hypixel:mod-api:$hypixel_mod_api_version")
    // The Hypixel Mod API Fabric mod itself, so runClient satisfies the "hypixel-mod-api" dependency in fabric.mod.json
    localRuntime("maven.modrinth:hypixel-mod-api:$hypixel_mod_api_fabric_version")


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


modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(System.getenv("MODRINTH_PROJECT_ID") ?: "skysens")
    versionNumber.set(project.version.toString())
    versionName.set("SkySens ${project.version}")
    versionType.set(System.getenv("MODRINTH_VERSION_TYPE") ?: "release")
    uploadFile.set(tasks.shadowJar)
    gameVersions.add(minecraft_version)
    loaders.add("fabric")
    changelog.set(System.getenv("CHANGELOG") ?: "")
    dependencies {
        required.project("fabric-api")
        required.project("fabric-language-kotlin")
        required.project("hypixel-mod-api")
    }
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

// ---------------------------------------------------------------------------------------------------------
// Generated GUI font definitions.
// Keep in sync with de.vantrex.skysens.client.ui.render.FontFamily (the MINECRAFT family has no JSON).
// One JSON per family x weight x oversample level (1..8): skysens:<prefix>_regular_N, <prefix>_semibold_N and
// <prefix>_semibold_display_N, each = a ttf provider + minecraft:default as the last (fallback) provider.
// ---------------------------------------------------------------------------------------------------------
data class GuiFont(val prefix: String, val size: Double = 9.0, val displaySize: Double = 14.0, val shiftY: Double = 0.0)

val guiFonts = listOf(
    GuiFont("inter"),
    GuiFont("geist"),
    GuiFont("ibm_plex_sans"),
    GuiFont("dm_sans"),
    GuiFont("manrope"),
    GuiFont("roboto"),
    GuiFont("nunito"),
    GuiFont("outfit"),
    GuiFont("poppins", size = 8.5, displaySize = 13.0),
    GuiFont("geist_mono", size = 8.5, displaySize = 13.0),
    GuiFont("jetbrains_mono", size = 8.5, displaySize = 13.0),
)
val guiFontMaxLevel = 8
val fontDefsDir = layout.buildDirectory.dir("generated/fontDefs")

val generateFontDefs = tasks.register("generateFontDefs") {
    group = "build"
    description = "Generates the GUI font definition JSONs (family x weight x oversample level)."
    val outDir = fontDefsDir
    val fonts = guiFonts
    val maxLevel = guiFontMaxLevel
    inputs.property("fonts", fonts.toString())
    inputs.property("maxLevel", maxLevel)
    outputs.dir(outDir)
    doLast {
        val dir = outDir.get().asFile.resolve("assets/skysens/font")
        dir.deleteRecursively()
        dir.mkdirs()
        fun fmt(d: Double) = d.toString()
        for (f in fonts) {
            val variants = listOf(
                Triple("regular", "regular", f.size),
                Triple("semibold", "semibold", f.size),
                Triple("semibold_display", "semibold", f.displaySize),
            )
            for ((name, ttf, size) in variants) {
                for (level in 1..maxLevel) {
                    dir.resolve("${f.prefix}_${name}_$level.json").writeText(
                        """
                        |{
                        |  "providers": [
                        |    {
                        |      "type": "ttf",
                        |      "file": "skysens:${f.prefix}_$ttf.ttf",
                        |      "size": ${fmt(size)},
                        |      "oversample": ${fmt(level.toDouble())},
                        |      "shift": [0.0, ${fmt(f.shiftY)}],
                        |      "skip": ""
                        |    },
                        |    {
                        |      "type": "reference",
                        |      "id": "minecraft:default"
                        |    }
                        |  ]
                        |}
                        |""".trimMargin()
                    )
                }
            }
        }
    }
}

sourceSets.named("client") {
    resources.srcDir(files(fontDefsDir).builtBy(generateFontDefs))
}
