pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
        mavenCentral()
    }

    // Plugin versions live in gradle.properties (constraint 7). The `plugins {}` block
    // cannot read `project.findProperty(...)`, so the versions are resolved here once
    // and the module scripts declare the plugin ids without a version.
    val loomVersion = providers.gradleProperty("loom_version").get()
    val lombokVersion = providers.gradleProperty("lombok_plugin_version").get()
    val shadowVersion = providers.gradleProperty("shadow_version").get()
    val springBootVersion = providers.gradleProperty("spring_boot_version").get()
    val springDependencyManagementVersion =
        providers.gradleProperty("spring_dependency_management_version").get()

    plugins {
        id("net.fabricmc.fabric-loom") version loomVersion
        id("io.freefair.lombok") version lombokVersion
        id("com.gradleup.shadow") version shadowVersion
        id("org.springframework.boot") version springBootVersion
        id("io.spring.dependency-management") version springDependencyManagementVersion
    }
}

rootProject.name = "skysens"

include("skysens-common")
include("skysens-mod")
include("skysens-server")
