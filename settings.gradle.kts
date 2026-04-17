pluginManagement {
    repositories {
        gradlePluginPortal {
            content {
                excludeGroup("org.apache.logging.log4j")
            }
        }
        mavenCentral()
        maven {
            setUrl("https://maven.arcseekers.com/releases")
        }
        maven {
            setUrl("https://maven.minecraftforge.net/")
        }
        maven {
            setUrl("https://maven.fabricmc.net/")
        }
        maven {
            setUrl("https://maven.wagyourtail.xyz/releases")
        }
    }
}

plugins {
    // Automatic toolchain provisioning
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Actinium"
