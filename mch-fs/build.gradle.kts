plugins {
    `maven-publish`
    id("java")
}

repositories {
    mavenLocal()
    mavenCentral()
}

version = property("mch_fs_version") as String

dependencies {
    implementation(project(":mch"))
    compileOnly("org.jetbrains:annotations:${property("jetbrains_annotations_version")}")
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
