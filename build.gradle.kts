import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    kotlin("jvm") version "2.4.10"
}

allprojects {
    group = "net.minevn"
    version = "26.3.1-free"

    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly(kotlin("stdlib"))
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        disableAutoTargetJvm()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(8)
    }

    kotlin.compilerOptions.jvmTarget = JvmTarget.JVM_1_8
}
