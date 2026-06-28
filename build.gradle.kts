plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("com.gradleup.shadow") version "9.4.1"
}

group = "fr.miuby.survi"
version = "5.0"

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
}

dependencies {
    paperweight.paperDevBundle("26.2.build.24-alpha")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")

    implementation("com.github.charry-gabriel:MiubyLib:v1.17")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Les tests n'ont pas accès au dev bundle → on re-déclare paper-api pour eux
    testImplementation("io.papermc.paper:paper-api:26.2.build.24-alpha")
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
    }
}
tasks.register<Exec>("deployMain") {
    dependsOn(tasks.shadowJar)

    commandLine(
        "scp",
        "-P", "2222",
        tasks.shadowJar.get().archiveFile.get().asFile.absolutePath,
        "admin@timeuhalefa.fr:/opt/minecraft/main/plugins/"
    )
}
tasks.register<Exec>("deployTest") {
    dependsOn(tasks.shadowJar)

    commandLine(
        "scp",
        "-P", "2222",
        tasks.shadowJar.get().archiveFile.get().asFile.absolutePath,
        "admin@timeuhalefa.fr:/opt/minecraft/test/plugins/"
    )
}