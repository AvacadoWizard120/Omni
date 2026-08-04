pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.legacyfabric.net/")
        maven("https://maven.quiltmc.org/repository/release/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases/")
        maven("https://maven.kikugie.dev/snapshots/")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

stonecutter {
    create(rootProject) {
        fun mc(version: String, vararg loaders: String) =
            loaders.forEach { loader ->
                version("$version-$loader", version).buildscript =
                    if (loader == "forge") {
                        when (version) {
                            "1.8", "1.8.8", "1.8.9", "1.9", "1.9.4", "1.10", "1.10.2", "1.11", "1.11.2", "1.12", "1.12.1", "1.12.2" -> "build.forge112.gradle.kts"
                            "1.20.1" -> "build.forge.gradle.kts"
                            else -> "build.modernforge.gradle"
                        }
                    } else {
                        "build.$loader.gradle.kts"
                    }
            }

        mc("1.3.1", "legacyfabric")
        mc("1.3.2", "legacyfabric")
        mc("1.4.2", "legacyfabric")
        mc("1.4.4", "legacyfabric")
        mc("1.4.5", "legacyfabric")
        mc("1.4.6", "legacyfabric")
        mc("1.4.7", "legacyfabric")
        mc("1.5.1", "legacyfabric")
        mc("1.5.2", "legacyfabric")
        mc("1.6.1", "legacyfabric")
        mc("1.6.2", "legacyfabric")
        mc("1.6.4", "liteloader", "legacyfabric")
        mc("1.7.2", "liteloader", "legacyfabric")
        mc("1.7.3", "legacyfabric")
        mc("1.7.4", "legacyfabric")
        mc("1.7.5", "legacyfabric")
        mc("1.7.6", "legacyfabric")
        mc("1.7.7", "legacyfabric")
        mc("1.7.8", "legacyfabric")
        mc("1.7.10", "liteloader", "legacyfabric")
        mc("1.8", "liteloader", "forge", "legacyfabric")
        mc("1.8.1", "legacyfabric")
        mc("1.8.2", "legacyfabric")
        mc("1.8.3", "legacyfabric")
        mc("1.8.4", "legacyfabric")
        mc("1.8.5", "legacyfabric")
        mc("1.8.6", "legacyfabric")
        mc("1.8.7", "legacyfabric")
        mc("1.8.8", "forge", "legacyfabric")
        mc("1.8.9", "liteloader", "forge", "legacyfabric")
        mc("1.9", "liteloader", "forge")
        mc("1.9.4", "liteloader", "forge", "legacyfabric")
        mc("1.10", "liteloader", "forge")
        mc("1.10.2", "liteloader", "forge", "legacyfabric")
        mc("1.11", "liteloader", "forge")
        mc("1.11.2", "liteloader", "forge", "legacyfabric")
        mc("1.12", "liteloader", "forge")
        mc("1.12.1", "liteloader", "forge")
        mc("1.12.2", "liteloader", "forge", "legacyfabric")
        mc("1.13.2", "legacyfabric")
        mc("1.14.4", "fabric", "forge")
        mc("1.15", "fabric", "forge")
        mc("1.15.1", "fabric", "forge")
        mc("1.15.2", "fabric", "forge")
        mc("1.16", "fabric")
        mc("1.16.1", "fabric")
        mc("1.16.2", "fabric", "forge")
        mc("1.16.3", "fabric", "forge")
        mc("1.16.4", "fabric", "forge")
        mc("1.16.5", "fabric", "forge")
        mc("1.17", "fabric")
        mc("1.17.1", "fabric", "forge")
        mc("1.18", "fabric", "forge")
        mc("1.18.1", "fabric", "forge")
        mc("1.18.2", "fabric", "forge")
        mc("1.19", "fabric", "forge")
        mc("1.19.1", "fabric", "forge")
        mc("1.19.2", "fabric", "forge")
        mc("1.19.3", "fabric", "forge")
        mc("1.19.4", "fabric", "forge")
        mc("1.20", "fabric", "forge")
        mc("1.20.1", "fabric", "quilt", "forge")
        mc("1.20.2", "fabric", "forge")
        mc("1.20.3", "fabric", "forge")
        mc("1.20.4", "fabric", "quilt", "forge", "neoforge")
        mc("1.20.5", "fabric")
        mc("1.20.6", "fabric", "forge")
        mc("1.21", "fabric", "forge")
        mc("1.21.1", "fabric", "quilt", "neoforge")
        mc("1.21.1", "forge")
        mc("1.21.2", "fabric", "quilt", "neoforge")
        mc("1.21.3", "fabric", "quilt", "forge", "neoforge")
        mc("1.21.4", "fabric", "quilt", "forge", "neoforge")
        mc("1.21.5", "fabric", "quilt", "forge", "neoforge")
        mc("1.21.6", "fabric", "quilt", "forge", "neoforge")
        mc("1.21.7", "fabric", "quilt", "forge", "neoforge")
        mc("1.21.8", "fabric", "quilt", "forge", "neoforge")
        mc("1.21.9", "fabric", "quilt", "forge", "neoforge")
        mc("1.21.10", "fabric", "quilt", "forge", "neoforge")
        mc("1.21.11", "fabric", "quilt", "forge", "neoforge")
        // Forge 26.1 fails during Forge's own FieldToMethodTransformer bootstrap
        // on both 62.0.8 and 62.0.9. Keep Fabric/NeoForge published.
        mc("26.1", "fabric", "quilt", "neoforge")
        mc("26.1.1", "fabric", "quilt", "forge", "neoforge")
        mc("26.1.2", "fabric", "quilt", "forge", "neoforge")
        mc("26.2", "fabric", "quilt", "forge", "neoforge")

        vcsVersion = "1.21.1-fabric"
    }
}

rootProject.name = "Omni-Overhaul"
