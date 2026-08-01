architectury {
    neoForge()
}

val common: Configuration by configurations.creating {
    configurations.compileClasspath.get().extendsFrom(this)
    configurations.runtimeClasspath.get().extendsFrom(this)
    configurations["developmentNeoForge"].extendsFrom(this)
}

dependencies {
    common(project(":common", configuration = "namedElements")) {
        isTransitive = false
    }
    shadowCommon(project(path = ":common", configuration = "transformProductionNeoForge")) {
        isTransitive = false
    }

    val minecraftVersion: String by project
    val neoforgeVersion: String by project
    val reiVersion: String by project

    neoForge(group = "net.neoforged", name = "neoforge", version = neoforgeVersion)

    forgeRuntimeLibrary("com.teamresourceful:bytecodecs:1.0.2")

    modLocalRuntime(group = "maven.modrinth", name = "xaeros-world-map", version = "neoforge-1.21.1-1.44.2")
    modLocalRuntime(group = "maven.modrinth", name = "xaeros-minimap", version = "neoforge-1.21.1-26.4.2")
}
