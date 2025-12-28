plugins {
    application
    id("com.gradleup.shadow") version "9.3.0"
}

application {
    mainClass.set("ca.bkaw.mch.cli.MchCli")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":mch"))
    implementation(project(":mch-hub-server"))
    implementation("com.google.inject:guice:6.0.0")
    implementation("info.picocli:picocli:4.7.4")
    annotationProcessor("info.picocli:picocli-codegen:4.7.4")
}

tasks.compileJava {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

tasks.shadowJar {
    archiveFileName = "mch-cli-all.jar"
}