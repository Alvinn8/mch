plugins {
    id("java")
}

version = property("mch_hub_server_version") as String

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":mch"))
    compileOnly("org.jetbrains:annotations:${property("jetbrains_annotations_version")}")
    implementation("org.json:json:${property("org_json_version")}")

    implementation("com.electronwill.night-config:core:${property("night_config_version")}")
    implementation("com.electronwill.night-config:toml:${property("night_config_version")}")
}
