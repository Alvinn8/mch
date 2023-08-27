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
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("commons-net:commons-net:3.9.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
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
}