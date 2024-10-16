plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":mch"))
    compileOnly("org.jetbrains:annotations:24.0.1")
    implementation("org.json:json:20240303")

    val nightConfigVersion = "3.8.1";
    implementation("com.electronwill.night-config:core:${nightConfigVersion}")
    implementation("com.electronwill.night-config:toml:${nightConfigVersion}")
}
