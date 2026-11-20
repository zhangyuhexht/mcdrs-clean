import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Kotlin 2.4.x + Gradle 9.x: compiles on JDK 25
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "top.mcdrsclean"
version = "1.21.0"
val epluginVer = "1.4.0"

// Paper 26.x switched to year.drop versions; artifacts no longer carry the -R0.1-SNAPSHOT suffix.
// Use "26.3.build.+" to follow the latest 26.3 build automatically.
val paperApiVer = "26.3.build.244-stable"

// EPlugin is a third-party Kotlin library; its coordinates and namespace belong upstream
// and are unrelated to this plugin, so they must not be renamed or dependency resolution breaks.
fun eplugin(module: String, version: String = epluginVer) = "top.e404:eplugin-$module:$version"

repositories {
    // eplugin is published in a private repo; install it to mavenLocal first
    mavenLocal()
    // papermc (paper-api 26.3)
    maven("https://repo.papermc.io/repository/maven-public/")
    // spigot (eplugin dependency)
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    // sonatype
    maven("https://oss.sonatype.org/content/groups/public/")
    // placeholderAPI
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    mavenCentral()
}

dependencies {
    // Paper 26.3 API (Java 25 bytecode)
    compileOnly("io.papermc.paper:paper-api:$paperApiVer")
    // eplugin
    implementation(eplugin("core"))
    implementation(eplugin("menu"))
    implementation(eplugin("serialization"))
    implementation(eplugin("hook-placeholderapi"))
    // placeholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6")
    // Bstats
    implementation("org.bstats:bstats-bukkit:3.0.2")

    // Tests still target MockBukkit 1.20 and are decoupled from the runtime core; use shadowJar for releases
    testImplementation(kotlin("test", "2.4.21"))
    testImplementation("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.87.0")
    testImplementation("org.slf4j:slf4j-simple:2.0.13")
    testImplementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    // Paper 26.1+ requires Java 25 and paper-api 26.3 is Java 25 bytecode (class file 69),
    // so compilation needs JDK 25; the output stays at Java 21 bytecode and runs fine on Java 25 servers.
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks {
    build {
        finalizedBy(shadowJar)
    }

    // jar and shadowJar share a name by default, so test/build would overwrite the shaded jar,
    // give the thin jar a classifier to protect the release artifact
    jar {
        archiveClassifier.set("plain")
    }

    shadowJar {
        val archiveName = "${project.name}-${project.version}.jar"
        archiveFileName.set(archiveName)

        relocate("org.bstats", "top.mcdrsclean.relocate.bstats")
        relocate("kotlin", "top.mcdrsclean.relocate.kotlin")
        relocate("top.e404.eplugin", "top.mcdrsclean.relocate.eplugin")
        relocate("com.charleskorn.kaml", "top.mcdrsclean.relocate.kaml")
        exclude("META-INF/**")

        doLast {
            val archiveFile = archiveFile.get().asFile
            println(archiveFile.parentFile.absolutePath)
            println(archiveFile.absolutePath)
        }
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    test {
        useJUnitPlatform()
        this.systemProperties["mcdrs.debug"] = true
    }
}
