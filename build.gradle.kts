plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
}


group = "ca.bkaw.mch"
version = "0.1.1"

tasks {
    runServer {
        minecraftVersion("1.21.11")
    }
}

subprojects {
    apply(plugin = "java-library")

    group = "ca.bkaw.mch"
    version = rootProject.version

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>() {
        options.encoding = "UTF-8"
        options.release = 17
    }

    tasks.withType<Javadoc>() {
        options.encoding = "UTF-8"
    }
}