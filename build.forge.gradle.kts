plugins {
    id("net.neoforged.moddev.legacyforge")
}

fun prop(name: String): String = project.property(name).toString()

val minecraft = prop("deps.minecraft")
val modId = prop("mod.id")
val modName = prop("mod.name")
val modVersion = prop("mod.version")
val modGroup = prop("mod.group")
val modDescription = prop("mod.description")
val modAuthor = prop("mod.author")
val modSources = prop("mod.sources")
val modIssues = prop("mod.issues")
val modHomepage = prop("mod.homepage")
val modLicense = prop("mod.license")
val modIcon = prop("mod.icon")
val versionRange = prop("version_range")
val javaVersion = prop("java_version").toInt()
val mixinCompatibilityLevel = prop("mixin_compatibility_level")

group = modGroup
base.archivesName = modId

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
    withSourcesJar()
}

version = "$modVersion+$minecraft-forge"

legacyForge {
    version = prop("deps.forge")

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }

    runs {
        register("client") {
            client()
            gameDirectory = file("run/")
        }
        register("server") {
            server()
            gameDirectory = file("run/")
        }
    }
}

mixin {
    add(sourceSets.main.get(), "${modId}.refmap.json")
    config("${modId}.mixins.json")
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

tasks.processResources {
    val props = mapOf(
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_version" to modVersion,
        "mod_description" to modDescription,
        "mod_author" to modAuthor,
        "mod_sources" to modSources,
        "mod_issues" to modIssues,
        "mod_homepage" to modHomepage,
        "mod_license" to modLicense,
        "mod_icon" to modIcon,
        "minecraft_version" to minecraft,
        "version_range" to versionRange,
        "loader_version_range" to prop("loader_version_range"),
        "forge_version_range" to prop("forge_version_range"),
        "mixin_compatibility_level" to mixinCompatibilityLevel
    )
    inputs.properties(props)
    filesMatching(listOf("META-INF/mods.toml", "${modId}.mixins.json")) {
        expand(props)
    }
    exclude("fabric.mod.json", "quilt.mod.json", "META-INF/neoforge.mods.toml")
}

tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}

tasks.named<Jar>("jar") {
    from(layout.buildDirectory.file("mixin/omni.refmap.json"))
}

val forgeRuntimeJar = layout.buildDirectory.file("libs/$modId-$modVersion+$minecraft-forge.jar")

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    dependsOn(tasks.named("build"), tasks.named("reobfJar"))
    from(forgeRuntimeJar)
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}

tasks.jar {
    manifest.attributes["MixinConfigs"] = "${modId}.mixins.json"
}
