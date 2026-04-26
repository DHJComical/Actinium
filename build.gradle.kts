import org.taumc.actinium.gradle.ActiniumUniminedHelper
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.compiler
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings
import java.io.File

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
    archivesName.set(propertyString("mod_name"))
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
        resources {
            exclude("mixins.celeritas.json")
        }
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

    remap(tasks.named<Jar>("jar").get())

    mods {
        val modCompileOnly by configurations.getting
        val modRuntimeOnly by configurations.getting
        remap(modCompileOnly)
        remap(modRuntimeOnly)
    }
}

apply(plugin = "dependencies")

ActiniumUniminedHelper.configureProductionRemap(project)
if (propertyBool("use_access_transformer")) {
    ActiniumUniminedHelper.configureSourceAccessTransformers(project, "src/main/resources/${propertyString("access_transformer_locations")}")
}

val generatedMixinConfigDir = layout.buildDirectory.dir("generated/actinium/mixins")

val generateMixinConfig by tasks.registering {
    val templateFile = layout.projectDirectory.file("src/main/resources/mixins.celeritas.json")
    val outputFile = generatedMixinConfigDir.map { it.file("mixins.celeritas.json") }

    inputs.file(templateFile)
    inputs.files(sourceSets.named("main").get().allJava.srcDirs)
    outputs.file(outputFile)

    doLast {
        @Suppress("UNCHECKED_CAST")
        val mixinConfig = JsonSlurper().parse(templateFile.asFile) as MutableMap<String, Any?>
        val mixinPackage = mixinConfig["package"] as? String
            ?: error("mixins.celeritas.json is missing a package field")
        val mixinPackagePath = mixinPackage.replace('.', '/') + "/"

        val mixinClasses = sourceSets.named("main").get().allJava.srcDirs
            .asSequence()
            .filter(File::exists)
            .flatMap { sourceRoot ->
                sourceRoot.walkTopDown()
                    .filter { it.isFile && it.extension == "java" }
                    .mapNotNull { sourceFile ->
                        val relativePath = sourceFile.relativeTo(sourceRoot).invariantSeparatorsPath
                        if (!relativePath.startsWith(mixinPackagePath)) {
                            return@mapNotNull null
                        }

                        if (!sourceFile.readText(Charsets.UTF_8).contains("@Mixin")) {
                            return@mapNotNull null
                        }

                        relativePath
                            .removePrefix(mixinPackagePath)
                            .removeSuffix(".java")
                            .replace('/', '.')
                    }
            }
            .distinct()
            .sorted()
            .toList()

        mixinConfig["client"] = mixinClasses

        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mixinConfig)) + System.lineSeparator(), Charsets.UTF_8)
    }
}

tasks.processResources {
    dependsOn(generateMixinConfig)
    val resourceProperties = mapOf(
        "mod_id" to propertyString("mod_id"),
        "mod_name" to propertyString("mod_name"),
        "mod_version" to propertyString("mod_version"),
        "mod_description" to propertyString("mod_description"),
        "mod_authors" to propertyString("mod_authors"),
        "mod_credits" to propertyString("mod_credits"),
        "mod_url" to propertyString("mod_url"),
        "mod_update_json" to propertyString("mod_update_json"),
        "mod_logo_path" to propertyString("mod_logo_path")
    )

    inputs.properties(resourceProperties)
    filteringCharset = "UTF-8"
    from(generateMixinConfig)

    filesMatching(listOf("mcmod.info", "pack.mcmeta")) {
        expand(resourceProperties)
        filter { line ->
            resourceProperties.entries.fold(line) { acc, (key, value) ->
                acc.replace("{{ $key }}", value).replace("{{${key}}}", value)
            }
        }
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
