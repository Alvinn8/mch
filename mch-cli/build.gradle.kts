plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("ca.bkaw.mch.cli.MchCli")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":mch"))
    implementation("net.sf.jopt-simple:jopt-simple:6.0-alpha-3")
}