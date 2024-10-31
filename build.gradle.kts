plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.7.4"
}

group = "net.okocraft.boxtradestick"
version = "1.6"

val mcVersion = "1.21.3"
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

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("io.papermc.paper:paper-api:$mcVersion-R0.1-SNAPSHOT")
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
