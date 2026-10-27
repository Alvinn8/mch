plugins {
    id("java")
    id("net.fabricmc.fabric-loom") version "1.18-SNAPSHOT"
}

version = "${property("mch_viewer_fabric_version")}+${property("minecraft_version")}"

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
    compileOnly("org.jetbrains:annotations:${property("jetbrains_annotations_version")}")

    // include is not transitive, so the dependencies of mch need to be included
    // explicitly.
    include("com.github.luben:zstd-jni:${property("zstd_version")}")
    include("org.json:json:${property("org_json_version")}")
    include("commons-net:commons-net:${property("commons_net_version")}")
    include("com.hierynomus:sshj:${property("sshj_version")}")

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
