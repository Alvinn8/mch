plugins {
    id("fabric-loom") version "1.5-SNAPSHOT"
    id("java")
}

base {
    archivesName = "mch-viewer-fabric"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.nucleoid.xyz/")
}

dependencies {
    // Fabric
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    // Fantasy for runtime worlds
    modImplementation("xyz.nucleoid:fantasy:${property("fantasy_version")}")

    // mch dependencies
    implementation(project(":mch"))
    implementation(project(":mch-fs"))
    compileOnly("org.jetbrains:annotations:24.0.1")
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand(getProperties())
            expand(mutableMapOf("version" to project.version))
        }
    }
}