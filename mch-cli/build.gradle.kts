plugins {
    application
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