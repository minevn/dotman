import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    kotlin("jvm") version "2.0.0"
}

allprojects {
    group = "net.minevn"
    version = "1.6"

    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly(kotlin("stdlib"))
    }

    java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    kotlin.compilerOptions.jvmTarget = JvmTarget.JVM_1_8
}
