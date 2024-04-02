plugins {
    id("java")
    id("fabric-loom") version "1.5-SNAPSHOT"
    id("com.github.johnrengelman.shadow") version "7.0.0"
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

    // mod dependencies
    include(modImplementation("xyz.nucleoid:fantasy:${property("fantasy_version")}")!!)
    include(modImplementation("net.kyori:adventure-platform-fabric:${property("adventure_version")}")!!)

    // mch dependencies
    implementation(project(":mch"))!!
    implementation(project(":mch-fs"))!!
    compileOnly("org.jetbrains:annotations:24.0.1")
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand(getProperties())
            expand(mutableMapOf("version" to project.version))
        }
    }

    shadowJar {
        archiveBaseName.set("mch-viewer-fabric-dev")
        archiveClassifier.set("")

        dependencies {
            exclude("net.fabricmc:.*")
            exclude("xyz.nucleoid:fantasy")
            include(dependency("ca.bkaw.mch:.*"))
            include(dependency("com.github.luben:zstd-jni:.*"))
            include(dependency("commons-net:commons-net:.*"))
            include(dependency("com.hierynomus:sshj:.*"))
            exclude("/mappings/*")
        }
    }

    val remappedShadowJar by registering(net.fabricmc.loom.task.RemapJarTask::class) {
        dependsOn(shadowJar)
        input = shadowJar.get().archiveFile
        addNestedDependencies = true
        archiveBaseName.set("mch-viewer-fabric")
    }

    assemble {
        dependsOn(remappedShadowJar)
    }

    artifacts {
        archives(remappedShadowJar)
        shadow(shadowJar)
    }
}
