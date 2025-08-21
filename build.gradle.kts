import xyz.jpenilla.runpaper.task.RunServer
import java.util.Calendar

plugins {
    `java-library`
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "9.0.0-beta16"
    id("com.github.hierynomus.license") version "0.16.1"
}

group = project.properties["maven_group"]!!
version = "v${project.properties["plugin_version"]}"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.13-R0.1-SNAPSHOT")
}

runPaper.folia.registerTask()

val mcVersion = "1.21.4"
val jvavVersion = JavaLanguageVersion.of(21)
val jvmArgsExternal = listOf(
    "-Dcom.mojang.eula.agree=true",
    "-XX:+AllowEnhancedClassRedefinition"
)

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {

    compileJava {
        options.encoding = "UTF-8"
        options.release = 8
    }
    javadoc {
        options.encoding = "UTF-8"
    }

    withType<RunServer> {
        minecraftVersion(mcVersion)
        jvmArgs = jvmArgsExternal
        javaLauncher = project.javaToolchains.launcherFor {
            @Suppress("UnstableApiUsage")
            vendor = JvmVendorSpec.JETBRAINS
            languageVersion = jvavVersion
        }
    }

    processResources {
        filesMatching("plugin.yml") {
            expand(
                "plugin_name" to project.properties["plugin_name"],
                "plugin_version" to project.properties["plugin_version"],
                "api_version" to project.properties["api_version"],
                "author" to project.properties["author"],
                "main_class" to "${project.properties["maven_group"]}.${project.properties["plugin_name"]}"
            )
        }
    }

    shadowJar {
        archiveClassifier.set("")
        from(rootProject.file("LICENSE")) {
            rename { "${it}_${project.properties["plugin_name"]}" }
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

license {
    // use "gradle licenseFormat" to apply license headers
    header = rootProject.file("HEADER.txt")
    include("**/*.java")
    skipExistingHeaders = true

    headerDefinitions.create("SLASHSTAR_STYLE_NEWLINE") {
        firstLine = "/*"
        beforeEachLine = " * "
        endLine = " */" + System.lineSeparator()
        afterEachLine = ""
        skipLinePattern = null
        firstLineDetectionPattern = """(\s|\t)*/\*.*$"""
        lastLineDetectionPattern = """.*\*/(\s|\t)*$"""
        allowBlankLines = false
        isMultiline = true
        padLines = false
    }
    mapping("java", "SLASHSTAR_STYLE_NEWLINE")

    ext["name"] = project.properties["plugin_name"]
    ext["author"] = project.properties["author"]
    ext["year"] = Calendar.getInstance().get(Calendar.YEAR).toString()
}