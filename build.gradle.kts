import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.lang.Closure


val pluginVersion: String by project.ext
val apiVersion: String by project.ext
val displayMonitorVersion: String by project.ext



plugins {
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "9.1.0"
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://libraries.minecraft.net")
    maven(url = "https://jitpack.io")
    maven {
        url = uri("https://maven.pkg.github.com/tororo1066/TororoPluginAPI")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }

    maven {
        url = uri("https://maven.pkg.github.com/tororo1066/DisplayMonitor")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(kotlin("reflect"))
    compileOnly("com.mojang:brigadier:1.0.18")
    compileOnly("io.papermc.paper:paper-api:$pluginVersion-R0.1-SNAPSHOT")
    implementation("tororo1066:tororopluginapi:$apiVersion")
    compileOnly("tororo1066:commandapi:${apiVersion}")
    compileOnly("tororo1066:base:${apiVersion}")

    compileOnly("tororo1066:display-monitor-api:$displayMonitorVersion")
    compileOnly("tororo1066:display-monitor-plugin:$displayMonitorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.22.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.22.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }

    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")

    testImplementation("tororo1066:display-monitor-api:$displayMonitorVersion")
    testImplementation("tororo1066:display-monitor-plugin:$displayMonitorVersion")
}


tasks.withType<ShadowJar> {
    archiveClassifier.set("")
}

tasks.test {
    useJUnitPlatform()
}