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
}