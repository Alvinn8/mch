repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":mch"))
    compileOnly("io.papermc.paper:paper-api:1.19-R0.1-SNAPSHOT")
}