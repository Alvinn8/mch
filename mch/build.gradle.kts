plugins {
    `maven-publish`
    id("jacoco-report-aggregation")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:${property("jetbrains_annotations_version")}")
    implementation("org.slf4j:slf4j-api:${property("slf4j_version")}")
    implementation("com.github.luben:zstd-jni:${property("zstd_version")}")
    implementation("org.json:json:${property("org_json_version")}")
    implementation("commons-net:commons-net:${property("commons_net_version")}") // FTP
    implementation("com.hierynomus:sshj:${property("sshj_version")}") // SFTP

    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit_jupiter_version")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junit_jupiter_version")}")
    testImplementation("org.mockftpserver:MockFtpServer:${property("mockftpserver_version")}")
}

tasks.test {
    useJUnitPlatform()
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