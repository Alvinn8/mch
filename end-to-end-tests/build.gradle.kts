plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":mch"))
    implementation(project(":mch-cli"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

tasks.test {
    useJUnitPlatform()
}
