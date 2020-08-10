@file:Suppress("UnstableApiUsage")

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

apply(plugin = "com.github.johnrengelman.shadow")

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    destinationDir = file("$rootDir/build/output")
    
    manifest {
        attributes["Main-Class"] = "net.mamoe.mirai.console.wrapper.WrapperMain"
    }
}


kotlin {
    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.Experimental")
            languageSettings.useExperimentalAnnotation("kotlin.OptIn")
        }
    }
}

dependencies {
    api(kotlin("stdlib", Versions.Kotlin.stdlib))
    api(kotlin("reflect", Versions.Kotlin.stdlib))

    api(kotlinx("coroutines-core", Versions.Kotlin.coroutines))
    api(kotlinx("coroutines-swing", Versions.Kotlin.coroutines))

    api(ktor("client-cio", Versions.Kotlin.ktor))
    api(ktor("client-core-jvm", Versions.Kotlin.ktor))
    api(ktor("network", Versions.Kotlin.ktor))

    api("com.github.ajalt:clikt:2.6.0")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))

    // implementation(kotlin("script-runtime"))
}

version = Versions.Mirai.consoleWrapper

description = "Bootstrap Wrapper for mirai-console"

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType(JavaCompile::class.java) {
    options.encoding = "UTF8"
}

tasks.withType(Jar::class.java) {
    destinationDir = file("$rootDir/build/output")      //$rootDir 取项目的顶级目录
}
