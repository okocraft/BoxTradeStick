import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.3.8"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "net.okocraft.boxtradestick"
version = "1.5"

repositories {
    mavenCentral()
    maven(url = "https://papermc.io/repo/repository/maven-public/")
    maven(url = "https://okocraft.github.io/Box/maven/")
}

dependencies {
    paperDevBundle("1.19.3-R0.1-SNAPSHOT")

    implementation("com.github.siroshun09.configapi:configapi-yaml:4.6.0")
    implementation("com.github.siroshun09.translationloader:translationloader:2.0.2")

    compileOnly("net.okocraft.box:box-api:5.2.0")
    compileOnly("net.okocraft.box:box-storage-api:5.2.0")
    compileOnly("net.okocraft.box:box-stick-feature:5.2.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named("build") {
    dependsOn(tasks.named("reobfJar"))
}

tasks.named<Copy>("processResources") {
    filesMatching(listOf("plugin.yml", "en.yml", "ja_JP.yml")) {
        expand("projectVersion" to version)
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.named<ShadowJar>("shadowJar") {
    minimize()
    relocate("com.github.siroshun09", "net.okocraft.boxtradestick.lib")
}
