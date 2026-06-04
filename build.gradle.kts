plugins {
    java
    // paperweight-userdev : donne accès à net.minecraft.*, com.mojang.authlib, PaperAdventure, etc.
    // Vérifier la dernière version compatible avec Paper 26.1 sur https://github.com/PaperMC/paperweight
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("com.gradleup.shadow") version "9.4.1"
}

group = "fr.miuby.survi"
version = "4.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.66-stable")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")

    implementation("com.github.charry-gabriel:MiubyLib:729256c74a")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Les tests n'ont pas accès au dev bundle → on re-déclare paper-api pour eux
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.66-stable")
    testImplementation("org.yaml:snakeyaml:2.3")
    compileOnly("org.apache.logging.log4j:log4j-core:2.19.0")
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    shadowJar {
        archiveClassifier = ""
        relocate("fr.miuby.lib", "fr.miuby.survi.shaded.lib")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }

    test {
        useJUnitPlatform()
    }

    build {
        dependsOn(shadowJar)
        // Pas de reobfJar nécessaire : Paper 26.1 tourne nativement avec les Mojang mappings
    }
}