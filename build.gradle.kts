import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Kotlin 2.4.x + Gradle 9.x: 支持在 JDK 25 上编译
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "top.mcdrsclean"
version = "1.21.0"
val epluginVer = "1.4.0"

// Paper 26.x 起版本号改为 year.drop 形式, artifact 不再带 -R0.1-SNAPSHOT 后缀.
// 若希望自动跟随 26.3 的最新构建, 可改为 "26.3.build.+".
val paperApiVer = "26.3.build.244-stable"

// EPlugin 是第三方 Kotlin 工具库, 其 Maven 坐标/类命名空间属于上游,
// 与 MCDRS Clean 自身的命名空间无关, 不能随本插件改名, 否则无法解析依赖.
fun eplugin(module: String, version: String = epluginVer) = "top.e404:eplugin-$module:$version"

repositories {
    // eplugin 由作者私有仓库发布, 需先安装到 mavenLocal 或保证该仓库可访问
    mavenLocal()
    // papermc (paper-api 26.3)
    maven("https://repo.papermc.io/repository/maven-public/")
    // spigot (eplugin 依赖)
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    // sonatype
    maven("https://oss.sonatype.org/content/groups/public/")
    // placeholderAPI
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    mavenCentral()
}

dependencies {
    // Paper 26.3 API (Java 25 字节码)
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

    // 测试套件仍基于 MockBukkit 1.20, 与运行时核心解耦, 构建发布包请用 shadowJar 任务
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
    // Paper 26.1 起要求 Java 25, paper-api 26.3 也是 Java 25 字节码(class file 69),
    // 因此编译必须使用 JDK 25; 产物保持 Java 21 字节码, 在 Java 25 服务端可正常运行.
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks {
    build {
        finalizedBy(shadowJar)
    }

    // 普通 jar 与 shadowJar 默认同名, 跑 test/build 时后者会被前者覆盖,
    // 给瘦 jar 加 classifier 避免发布产物被意外替换
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
