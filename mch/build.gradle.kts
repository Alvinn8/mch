plugins {
    `maven-publish`
    id("jacoco-report-aggregation")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
    implementation("com.github.luben:zstd-jni:1.5.5-5")
    implementation("org.json:json:20240303")
    implementation("commons-net:commons-net:3.9.0") // FTP
    implementation("com.hierynomus:sshj:0.38.0") // SFTP

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.mockftpserver:MockFtpServer:3.1.0")
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
            name = "GitHubPackages"
            val githubRepo = System.getenv("GITHUB_REPOSITORY") ?: ""
            val parts = githubRepo.split("/")
            val owner = if (parts.size >= 2) parts[0] else "OWNER"
            val repo = if (parts.size >= 2) parts[1] else "REPO"
            url = uri("https://maven.pkg.github.com/$owner/$repo")

            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}