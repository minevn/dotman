plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases")
    maven("https://repo.minevn.net/releases/")
}

dependencies {
    // spigot
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")

    // libs
    compileOnly("net.minevn:minevnlib-plugin:1.2.1-beta3")
    compileOnly("minevn.depend:playerpoints:3.2.6")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("org.bstats:bstats-bukkit:3.0.2")

    // JUnit
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-junit-jupiter:5.4.0")
    testImplementation("io.mockk:mockk:1.13.7")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get())
}

tasks {
    test {
        useJUnitPlatform()
    }

    val jarName = "DotMan"

    register("customCopy") {
        dependsOn(shadowJar)

        val path = project.properties["shadowPath"]
        if (path != null) {
            doLast {
                println(path)
                copy {
                    from("build/libs/$jarName.jar")
                    into(path)
                }
                println("Copied")
            }
        }
    }

    processResources {
        outputs.upToDateWhen { false }
        filesMatching(listOf("**/plugin.yml")) {
            expand(mapOf("version" to project.version.toString()))
            println("$name: set version to ${project.version}")
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }

    shadowJar {
        relocate("org.bstats", "net.minevn.bstats")
        archiveFileName.set("$jarName.jar")
    }

    assemble {
        dependsOn(shadowJar, get("customCopy"))
    }
}
