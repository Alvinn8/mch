plugins {
    java
}


group = "ca.bkaw.mch"
version = "0.1-SNAPSHOT"

subprojects {
    apply(plugin = "java-library")

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