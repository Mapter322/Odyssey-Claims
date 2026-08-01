architectury {
    val enabledPlatforms: String by rootProject
    common(enabledPlatforms.split(","))
}

repositories {
    maven {
        name = "JourneyMap (Public)"
        url = uri("https://jm.gserv.me/repository/maven-public/")
    }
    maven {
        name = "Xaero (Chocolate)"
        url = uri("https://chocolateminecraft.com/maven/")
    }
    mavenCentral()
}

dependencies {
    modCompileOnly(group = "tech.thatgravyboat", name = "commonats", version = "2.0")
    modCompileOnly(group = "maven.modrinth", name = "xaeros-world-map", version = "fabric-1.21.1-1.44.2")
    modCompileOnly(group = "xaero.lib", name = "xaerolib-fabric-1.21.1", version = "1.7.1")
}