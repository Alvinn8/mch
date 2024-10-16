include("mch")
include("mch-cli")
include("mch-fs")
include("mch-hub-server")

include("mch-viewer-fabric")
project(":mch-viewer-fabric").projectDir = file("mch-viewer/fabric")

pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
}
