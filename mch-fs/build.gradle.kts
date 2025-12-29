plugins {
    `maven-publish`
    id("java")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":mch"))
    compileOnly("org.jetbrains:annotations:24.0.1")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }

    repositories {
        maven {
            val isSnapshot = version.toString().endsWith("-SNAPSHOT")
            url = uri(
                if (isSnapshot) {
                    "https://maven.bkaw.ca/repository/maven-snapshots/"
                } else {
                    "https://maven.bkaw.ca/repository/maven-releases/"
                }
            )
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}
