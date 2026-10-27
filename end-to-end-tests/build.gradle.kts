plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":mch"))
    implementation(project(":mch-cli"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit_jupiter_version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junit_jupiter_version")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
