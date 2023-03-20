import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.5.3"
    id("com.github.johnrengelman.shadow") version "8.1.0"
}

group = "net.okocraft.boxtradestick"
version = "1.5"

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://okocraft.github.io/Box/maven/")
}

dependencies {
    paperweight.paperDevBundle("1.19.4-R0.1-20230319.193530-16")

    implementation("com.github.siroshun09.configapi:configapi-yaml:4.6.3")
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
