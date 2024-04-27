plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.6.0"
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "net.okocraft.boxtradestick"
version = "1.6-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://okocraft.github.io/Box/maven/")
}

dependencies {
    paperweight.paperDevBundle("1.20.5-R0.1-SNAPSHOT")

    implementation("com.github.siroshun09.configapi:configapi-yaml:4.6.4")
    implementation("com.github.siroshun09.translationloader:translationloader:2.0.2")

    compileOnly("net.okocraft.box:box-api:5.5.2")
    compileOnly("net.okocraft.box:box-storage-api:5.5.2")
    compileOnly("net.okocraft.box:box-stick-feature:5.5.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("io.papermc.paper:paper-api:1.20.5-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching(listOf("plugin.yml", "en.yml", "ja_JP.yml")) {
            expand("projectVersion" to version)
        }
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        minimize()
        relocate("com.github.siroshun09", "net.okocraft.boxtradestick.lib")
    }
}
