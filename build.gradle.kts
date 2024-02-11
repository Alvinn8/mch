plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.1.0"
}


group = "ca.bkaw.mch"
version = "0.1-SNAPSHOT"

tasks {
    runServer {
        minecraftVersion("1.20.1")
    }
}

subprojects {
    apply(plugin = "java-library")

    group = "ca.bkaw.mch"
    version = "0.1-SNAPSHOT"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.withType<JavaCompile>() {
        options.encoding = "UTF-8"
    }

    tasks.withType<Javadoc>() {
        options.encoding = "UTF-8"
    }
}