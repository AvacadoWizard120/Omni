plugins {
    java
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
val javaVersion = prop("java_version").toInt()

group = modGroup
base.archivesName = modId
version = "$modVersion+$minecraft-forge"

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
    withSourcesJar()
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf(rootProject.file("src/forge112/java"), rootProject.file("src/liteloader/java")))
        java.exclude("**/LiteModOmni.java")
        resources.setSrcDirs(listOf(rootProject.file("src/forge112/resources"), rootProject.file("src/main/resources")))
    }
}

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    compileOnly("net.minecraftforge:forge:${prop("deps.forge")}:universal")
    compileOnly("net.minecraft:launchwrapper:1.12")
    compileOnly("org.ow2.asm:asm-debug-all:5.2")
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
        "minecraft_version" to minecraft
    )
    inputs.properties(props)
    filesMatching("mcmod.info") {
        expand(props)
    }
    exclude("fabric.mod.json", "quilt.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "${modId}.mixins.json", "pack.mcmeta")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "FMLCorePlugin" to "io.github.avacadowizard120.omni.forge112.OmniForgeLoadingPlugin",
            "FMLCorePluginContainsFMLMod" to "true",
            "ForceLoadAsMod" to "true"
        )
    }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    dependsOn(tasks.named("build"))
    from(tasks.named<Jar>("jar").map { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}
