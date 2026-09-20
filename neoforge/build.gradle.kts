architectury {
    neoForge()
}

repositories {
    maven(url = "https://maven.createmod.net")
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
    val createVersion: String by project
    val ponderVersion: String by project
    val cbcVersion: String by project

    neoForge(group = "net.neoforged", name = "neoforge", version = neoforgeVersion)

    modCompileOnly(group = "maven.modrinth", name = "create", version = createVersion) {
        isTransitive = false
    }

    modCompileOnly(group = "net.createmod.ponder", name = "Ponder-NeoForge-$minecraftVersion", version = ponderVersion) {
        isTransitive = false
    }

    modCompileOnly(group = "maven.modrinth", name = "create-big-cannons", version = cbcVersion) {
        isTransitive = false
    }

    forgeRuntimeLibrary("com.teamresourceful:bytecodecs:1.0.2")

    modLocalRuntime(group = "maven.modrinth", name = "xaeros-world-map", version = "neoforge-1.21.1-1.44.2")
    modLocalRuntime(group = "maven.modrinth", name = "xaeros-minimap", version = "neoforge-1.21.1-26.4.2")
}
