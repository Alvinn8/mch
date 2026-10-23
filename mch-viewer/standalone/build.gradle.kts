plugins {
    application
    id("com.gradleup.shadow") version "9.3.0"
}

application {
    mainClass.set("ca.bkaw.mch.viewer.standalone.MchViewerStandalone")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":mch"))
    implementation("org.json:json:20240303")
}


tasks.shadowJar {
    archiveFileName = "mch-viewer-standalone-all-${version}.jar"
}
