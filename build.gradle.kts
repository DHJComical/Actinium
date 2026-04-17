import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.compiler
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    java
    `java-library`
    `maven-publish`
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.4.1"
    id("xyz.wagyourtail.unimined") version "1.4.16-kappa"
}

assertProperty("mod_version")
assertProperty("root_package")
assertProperty("mod_id")
assertProperty("mod_name")
assertSubProperties("use_access_transformer", "access_transformer_locations")
assertSubProperties("is_coremod", "coremod_includes_mod", "coremod_plugin_class_name")

setDefaultProperty("generate_sources_jar", true, true)
setDefaultProperty("generate_javadocs_jar", true, false)
setDefaultProperty("minecraft_username", true, "Developer")
setDefaultProperty("extra_jvm_args", false, "")

version = propertyString("mod_version")
group = propertyString("root_package")

base {
    archivesName.set(propertyString("mod_id"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    if (propertyBool("generate_sources_jar")) {
        withSourcesJar()
    }
    if (propertyBool("generate_javadocs_jar")) {
        withJavadocJar()
    }
}

configurations {
    val contain by creating
    implementation {
        extendsFrom(contain)
    }
    val modCompileOnly by creating
    compileOnly {
        extendsFrom(modCompileOnly)
    }
    val modRuntimeOnly by creating
    runtimeOnly {
        extendsFrom(modRuntimeOnly)
    }
}

sourceSets {
    named("main") {
        java.srcDir("src/lwjglCommon/java")
        java.srcDir("src/lwjgl3/java")
    }
}

repositories {
    mavenCentral()
    maven("https://maven.cleanroommc.com")
    maven("https://cursemaven.com")
    maven("https://api.modrinth.com/maven")
    maven("https://nexus.gtnewhorizons.com/repository/public/")
    mavenLocal()
}

unimined.minecraft {
    version("1.12.2")

    mappings {
        mcp("stable", "39-1.12")
    }

    cleanroom {
        if (propertyBool("use_access_transformer")) {
            accessTransformer("${rootProject.projectDir}/src/main/resources/${propertyString("access_transformer_locations")}")
        }
        loader("0.5.6-alpha")
        runs.auth.username = property("minecraft_username").toString()
        runs.all {
            val extraArgs = propertyString("extra_jvm_args")
            if (extraArgs.isNotBlank()) {
                jvmArgs(extraArgs.split("\\s+".toRegex()))
            }
            if (propertyBool("enable_foundation_debug")) {
                systemProperties.apply {
                    set("foundation.dump", "true")
                    set("foundation.verbose", "true")
                }
            }
            if (propertyBool("is_coremod")) {
                systemProperty("fml.coreMods.load", propertyString("coremod_plugin_class_name"))
            }
        }
    }

    defaultRemapJar = false

    remap(tasks.named<Jar>("jar").get()) {
        mixinRemap {
            enableBaseMixin()
            disableRefmap()
        }
    }

    mods {
        val modCompileOnly by configurations.getting
        val modRuntimeOnly by configurations.getting
        remap(modCompileOnly)
        remap(modRuntimeOnly)
    }
}

apply(plugin = "dependencies")

tasks.processResources {
    inputs.property("version", version)
    filesMatching("mcmod.info") {
        expand(mapOf("version" to project.version.toString()))
    }
}

idea {
    module {
        inheritOutputDirs = true
    }
    project {
        settings {
            runConfigurations {
                add(Gradle("1. Build").apply {
                    setProperty("taskNames", listOf("build"))
                })
                add(Gradle("2. Run Client").apply {
                    setProperty("taskNames", listOf("runClient"))
                })
                add(Gradle("3. Run Server").apply {
                    setProperty("taskNames", listOf("runServer"))
                })
            }
            compiler.javac {
                afterEvaluate {
                    javacAdditionalOptions = "-encoding utf8"
                }
            }
        }
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val contain by configurations.getting
    if (!contain.isEmpty) {
        into("/") {
            from(contain)
        }
    }

    doFirst {
        manifest {
            val attributeMap = mutableMapOf<String, Any>(
                "ModType" to "CRL"
            )
            if (!contain.isEmpty) {
                attributeMap["ContainedDeps"] = contain.joinToString(" ") { it.name }
                attributeMap["NonModDeps"] = true
            }
            if (propertyBool("is_coremod")) {
                attributeMap["FMLCorePlugin"] = propertyString("coremod_plugin_class_name")
                attributeMap["ForceLoadAsMod"] = "true"
                if (propertyBool("coremod_includes_mod")) {
                    attributeMap["FMLCorePluginContainsFMLMod"] = true
                }
            }
            if (propertyBool("use_access_transformer")) {
                attributeMap["FMLAT"] = propertyString("access_transformer_locations").substringAfterLast('/')
            }
            attributes(attributeMap)
        }
    }

    finalizedBy(tasks.named("remapJar"))
}

tasks.named<Jar>("sourcesJar") {
    from("src/lwjglCommon/java")
    from("src/lwjgl3/java")
}

tasks.named("remapJar") {
    doFirst {
        logging.captureStandardOutput(LogLevel.INFO)
    }
    doLast {
        logging.captureStandardOutput(LogLevel.QUIET)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.encoding = "UTF-8"
    options.release.set(21)
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.test {
    useJUnitPlatform()
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    if (propertyBool("show_testing_output")) {
        testLogging {
            showStandardStreams = true
        }
    }
}

apply(plugin = "publishing")
apply(plugin = "extra")
