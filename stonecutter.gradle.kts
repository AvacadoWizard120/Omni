plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.14-SNAPSHOT" apply false
    id("legacy-looming") version "1.14-SNAPSHOT" apply false
    id("org.quiltmc.loom") version "1.15.1" apply false
    id("net.minecraftforge.gradle") version "7.0.31" apply false
    id("net.neoforged.moddev") version "2.0.147" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.147" apply false
}

stonecutter active "1.21.1-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('-'), "fabric", "quilt", "neoforge", "forge", "liteloader", "legacyfabric")
    constants.put("client_input", eval(node.metadata.version, ">=1.21.2"))
    constants.put("legacy_input", eval(node.metadata.version, "<1.21.2"))
    constants.put("impulse_input", eval(node.metadata.version, "<1.21.5"))
    constants.put("vector_input", eval(node.metadata.version, ">=1.21.5"))
    constants.put("getter_y_rot", eval(node.metadata.version, ">=1.17"))
    constants.put("field_y_rot", eval(node.metadata.version, "<1.17"))
    constants.put("no_mappings", eval(node.metadata.version, ">=26"))
    constants.put("legacy_forge_pre17", node.metadata.project.endsWith("-forge") && eval(node.metadata.version, ">=1.14.4") && eval(node.metadata.version, "<1.17.1"))
    constants.put("legacy_forge_vec3d", node.metadata.project.endsWith("-forge") && eval(node.metadata.version, ">=1.14.4") && eval(node.metadata.version, "<1.16"))
    constants.put("legacy_forge_vector3d", node.metadata.project.endsWith("-forge") && eval(node.metadata.version, ">=1.16") && eval(node.metadata.version, "<1.17.1"))
    constants.put("modern_forge", node.metadata.project.endsWith("-forge") && node.metadata.version != "1.20.1" && eval(node.metadata.version, ">=1.17.1") && eval(node.metadata.version, "<26"))
    constants.put("forge_reflective_access", node.metadata.project.endsWith("-forge") && node.metadata.version != "1.20.1" && eval(node.metadata.version, ">=1.14.4") && eval(node.metadata.version, "<26"))
    constants.put("modern_forge_impulse", node.metadata.project.endsWith("-forge") && node.metadata.version != "1.20.1" && eval(node.metadata.version, ">=1.17.1") && eval(node.metadata.version, "<1.21.5"))
    constants.put("modern_forge_vector", node.metadata.project.endsWith("-forge") && eval(node.metadata.version, ">=1.21.5") && eval(node.metadata.version, "<26"))
    constants.put("forge_packet_command_aliases", node.metadata.project.endsWith("-forge") && eval(node.metadata.version, ">=1.19.3") && eval(node.metadata.version, "<26"))
    constants.put("packet_command", eval(node.metadata.version, ">=1.19.3"))
    constants.put("unsigned_packet_command", eval(node.metadata.version, ">=1.19.3") && eval(node.metadata.version, "<26"))
    constants.put("legacy_chat_command", eval(node.metadata.version, "<1.19"))
    constants.put("command_method", eval(node.metadata.version, ">=1.19") && eval(node.metadata.version, "<1.19.1"))
    constants.put("signed_command_method", eval(node.metadata.version, ">=1.19.1") && eval(node.metadata.version, "<1.19.3"))
}

val pythonExecutable = providers.gradleProperty("python.executable")
    .orElse(providers.environmentVariable("PYTHON"))
    .orElse("python")

val collectTasks = subprojects.map { "${it.path}:buildAndCollect" }

tasks.register<Exec>("modrinthPlan") {
    group = "publishing"
    description = "Builds collected artifacts and writes a dry-run Modrinth upload plan."
    dependsOn(collectTasks)
    workingDir = rootProject.projectDir
    commandLine(pythonExecutable.get(), "tools/modrinth_publish.py")
}

tasks.register<Exec>("modrinthValidate") {
    group = "publishing"
    description = "Builds collected artifacts and validates planned Modrinth loaders/game versions."
    dependsOn(collectTasks)
    workingDir = rootProject.projectDir
    commandLine(pythonExecutable.get(), "tools/modrinth_publish.py", "--validate-tags")
}

tasks.register<Exec>("modrinthPublish") {
    group = "publishing"
    description = "Builds collected artifacts and uploads them to Modrinth."
    dependsOn(collectTasks)
    workingDir = rootProject.projectDir
    commandLine(pythonExecutable.get(), "tools/modrinth_publish.py", "--upload")
}

tasks.register<Exec>("curseforgePlan") {
    group = "publishing"
    description = "Builds collected artifacts and writes a dry-run CurseForge upload plan."
    dependsOn(collectTasks)
    workingDir = rootProject.projectDir
    commandLine(pythonExecutable.get(), "tools/curseforge_publish.py")
}

tasks.register<Exec>("curseforgeValidate") {
    group = "publishing"
    description = "Builds collected artifacts and validates planned CurseForge game version IDs."
    dependsOn(collectTasks)
    workingDir = rootProject.projectDir
    commandLine(pythonExecutable.get(), "tools/curseforge_publish.py", "--validate")
}

tasks.register<Exec>("curseforgePublish") {
    group = "publishing"
    description = "Builds collected artifacts and uploads them to CurseForge."
    dependsOn(collectTasks)
    workingDir = rootProject.projectDir
    commandLine(pythonExecutable.get(), "tools/curseforge_publish.py", "--upload")
}

tasks.register<Exec>("runClientMatrixPlan") {
    group = "verification"
    description = "Prints the queued runClient targets without launching Minecraft."
    workingDir = rootProject.projectDir
    commandLine(pythonExecutable.get(), "tools/run_client_matrix.py", "--dry-run")
}

tasks.register<Exec>("runClientMatrix") {
    group = "verification"
    description = "Runs every launchable runClient target one at a time, stopping on the first bad exit code."
    workingDir = rootProject.projectDir
    commandLine(pythonExecutable.get(), "tools/run_client_matrix.py")
}
