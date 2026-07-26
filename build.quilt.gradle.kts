plugins {
    id("org.quiltmc.loom")
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
val noIntermediateMappings = project.findProperty("loom.no_intermediate_mappings")?.toString()?.toBoolean() ?: false
val mappingsDependency = project.findProperty("deps.mappings")?.toString()

group = modGroup
base.archivesName = modId

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
    if (!noIntermediateMappings) {
        withSourcesJar()
    }
}

version = "$modVersion+$minecraft-quilt"

repositories {
    mavenCentral()
    maven("https://maven.quiltmc.org/repository/release/")
}

loom {
    if (noIntermediateMappings) {
        noIntermediateMappings()
    }

    val accessWidener = rootProject.file("src/main/resources/${modId}.accesswidener")
    if (accessWidener.exists()) {
        accessWidenerPath = accessWidener
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    if (mappingsDependency != null) {
        mappings(mappingsDependency)
    } else {
        mappings(loom.officialMojangMappings())
    }
    modImplementation("org.quiltmc:quilt-loader:${prop("deps.quilt-loader")}")
}

tasks.processResources {
    val props = mapOf(
        "mod_id" to modId,
        "mod_group" to modGroup,
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
        "quilt_loader_version" to prop("deps.quilt-loader"),
        "mixin_compatibility_level" to mixinCompatibilityLevel
    )
    inputs.properties(props)
    filesMatching(listOf("quilt.mod.json", "${modId}.mixins.json")) {
        expand(props)
    }
    exclude("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml")
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    dependsOn(tasks.named("remapJar"))
    from(tasks.named("remapJar"))
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}
