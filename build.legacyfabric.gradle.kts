plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("legacy-looming")
}

fun prop(name: String): String = project.property(name).toString()

val minecraft = prop("deps.minecraft")
val yarnBuild = prop("deps.yarn-build")
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
val javaVersion = prop("java_version").toInt()
val sprintMixin = prop("sprint_mixin")
val movementMixin = prop("movement_mixin")
val jumpMixin = prop("jump_mixin")
val commandMixin = prop("command_mixin")

group = modGroup
base.archivesName = modId
version = "$modVersion+$minecraft-legacyfabric"

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
    withSourcesJar()
}

sourceSets {
    named("main") {
        java.setSrcDirs(
            listOf(
                rootProject.file("src/legacyfabric/java"),
                rootProject.file("src/liteloader/java")
            )
        )
        java.exclude("**/liteloader/**")
        listOf("LegacyLivingEntityClassicMixin", "LegacyLivingEntityModernMixin", "LegacyMobEntityClassicMixin")
            .filterNot { it == movementMixin }
            .forEach { java.exclude("**/$it.java") }
        listOf("LegacyClientPlayerMixin", "LegacyClientPlayerNoSprintKeyMixin", "LegacyClientPlayerMuseumMixin")
            .filterNot { it == sprintMixin }
            .forEach { java.exclude("**/$it.java") }
        listOf("LegacyLivingEntityJumpMixin", "LegacyMobEntityJumpMixin")
            .filterNot { it == jumpMixin }
            .forEach { java.exclude("**/$it.java") }
        listOf("LegacyClientCommandMixin", "LegacyClientCommand17Mixin", "LegacyClientCommandEarlyMixin", "LegacyClientCommandMuseumMixin")
            .filterNot { it == commandMixin }
            .forEach { java.exclude("**/$it.java") }
        if (movementMixin == "LegacyMobEntityClassicMixin") {
            java.exclude("**/LegacyLivingMovement.java")
        } else {
            java.exclude("**/LegacyMobMovement.java")
        }
        resources.setSrcDirs(listOf(rootProject.file("src/legacyfabric/resources")))
    }
}

repositories {
    mavenCentral()
    maven("https://maven.legacyfabric.net/")
}

loom {
    if (minecraft in setOf("1.7.2", "1.7.3", "1.7.4", "1.7.5", "1.7.6", "1.7.7", "1.7.8", "1.7.10")) {
        runs.named("client") {
            programArgs("--userProperties", "{}")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings("net.legacyfabric:yarn:$minecraft+build.$yarnBuild:v2")
    modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
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
        "fabric_loader_min_version" to prop("fabric_loader_min_version"),
        "mixin_compatibility_level" to prop("mixin_compatibility_level"),
        "sprint_mixin" to sprintMixin,
        "movement_mixin" to movementMixin,
        "jump_mixin" to jumpMixin,
        "command_mixin" to commandMixin
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "${modId}.legacyfabric.mixins.json")) {
        expand(props)
    }
    from(rootProject.file("src/main/resources/assets")) {
        into("assets")
    }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    dependsOn(tasks.named("remapJar"))
    from(tasks.named("remapJar"))
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}
