plugins {
    java
    alias(libs.plugins.paperweight.userdev)
}

group = "net.okocraft.boxtradestick"
version = "1.7"

val mcVersion = libs.versions.paper.get().replaceAfter(".build", "").removeSuffix(".build")
val fullVersion = "${version}-mc${mcVersion}"

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://okocraft.github.io/Box/maven/")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper.get())

    compileOnly("net.okocraft.box:box-api:6.0.0-rc.4")
    compileOnly("net.okocraft.box:box-gui-feature:6.0.0-rc.4")
    compileOnly("net.okocraft.box:box-stick-feature:6.0.0-rc.4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testRuntimeOnly("io.papermc.paper:paper-api:${libs.versions.paper.get()}")
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
