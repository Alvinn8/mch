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
    compileOnly("org.jetbrains:annotations:${property("jetbrains_annotations_version")}")
    implementation("com.google.inject:guice:${property("guice_version")}")
    implementation("com.google.guava:guava:${property("guava_version")}")
    implementation("info.picocli:picocli:${property("picocli_version")}")
    runtimeOnly("org.slf4j:slf4j-simple:${property("slf4j_version")}")
    annotationProcessor("info.picocli:picocli-codegen:${property("picocli_version")}")
}

tasks.compileJava {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

tasks.shadowJar {
    archiveFileName = "mch-cli-all-${version}.jar"
}

tasks.jar {
    manifest {
        // Allow zstd-jni to load its native library
        // TODO when https://github.com/luben/zstd-jni/issues/371 is resolved
        //  replace this with a more specific module name.
        attributes("Enable-Native-Access" to "ALL-UNNAMED")
    }
}
