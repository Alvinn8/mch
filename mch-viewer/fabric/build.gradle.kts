plugins {
    id("java")
    id("net.fabricmc.fabric-loom") version "1.18-SNAPSHOT"
}

version = "0.1+${property("minecraft_version")}"

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
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    // mod dependencies
    include(implementation("xyz.nucleoid:fantasy:${property("fantasy_version")}")!!)
    include(implementation("net.kyori:adventure-platform-fabric:${property("adventure_version")}")!!)

    // mch dependencies
    include(implementation(project(":mch"))!!)
    include(implementation(project(":mch-fs"))!!)
    compileOnly("org.jetbrains:annotations:24.0.1")

    // include is not transitive, so the dependencies of mch need to be included
    // explicitly. Keep these versions in sync with mch/build.gradle.kts
    // TODO add gradle.properties for these?
    include("com.github.luben:zstd-jni:1.5.5-5")
    include("org.json:json:20240303")
    include("commons-net:commons-net:3.9.0")
    include("com.hierynomus:sshj:0.38.0")

    include(implementation("com.electronwill.night-config:core:${property("night_config_version")}")!!)
    include(implementation("com.electronwill.night-config:toml:${property("night_config_version")}")!!)
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand(getProperties())
            expand(mutableMapOf("version" to project.version))
        }
    }
}
