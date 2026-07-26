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
val modHomepage = prop("mod.homepage")
val javaVersion = prop("java_version").toInt()

group = modGroup
base.archivesName = modId
version = "$modVersion+$minecraft-liteloader"

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
    withSourcesJar()
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf(rootProject.file("src/liteloader/java")))
        resources.setSrcDirs(listOf(rootProject.file("src/liteloader/resources")))
    }
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net/")
    maven("https://repo.spongepowered.org/maven/")
    maven("http://repo.mumfrey.com/content/repositories/snapshots/") {
        isAllowInsecureProtocol = true
    }
}

dependencies {
    compileOnly("com.mumfrey:liteloader:${prop("deps.liteloader")}")
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
        "mod_homepage" to modHomepage,
        "minecraft_version" to minecraft
    )
    inputs.properties(props)
    filesMatching("litemod.json") {
        expand(props)
    }
}

tasks.named<Jar>("jar") {
    archiveExtension.set("litemod")
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    dependsOn(tasks.named("build"))
    from(tasks.named<Jar>("jar").map { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}
