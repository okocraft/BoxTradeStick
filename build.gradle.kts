plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

group = "net.okocraft.boxtradestick"
version = "1.6"

val mcVersion = "1.21.9"
val fullVersion = "${version}-mc${mcVersion}"

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://okocraft.github.io/Box/maven/")
}

dependencies {
    paperweight.paperDevBundle("$mcVersion-R0.1-SNAPSHOT")

    compileOnly("net.okocraft.box:box-api:6.0.0-rc.2")
    compileOnly("net.okocraft.box:box-gui-feature:6.0.0-rc.2")
    compileOnly("net.okocraft.box:box-stick-feature:6.0.0-rc.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
    testRuntimeOnly("io.papermc.paper:paper-api:$mcVersion-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }

    processResources {
        filesMatching(listOf("paper-plugin.yml")) {
            expand("projectVersion" to version, "apiVersion" to mcVersion)
        }
    }

    test {
        useJUnitPlatform()
    }

    jar {
        archiveFileName = "BoxTradeStick-$fullVersion.jar"
    }
}
