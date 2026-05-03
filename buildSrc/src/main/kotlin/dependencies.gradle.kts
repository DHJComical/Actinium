plugins {
    java
}

repositories {
    // Other repositories described by default:
    // CleanroomMC: https://maven.cleanroommc.com
    mavenCentral()
    maven {
        name = "CurseMaven"
        setUrl("https://cursemaven.com")
    }
    maven {
        name = "CleanroomCurseMaven"
        setUrl("https://curse.cleanroommc.com")
    }
    maven {
        name = "Modrinth"
        setUrl("https://api.modrinth.com/maven")
    }
    maven {
        name = "CleanroomMaven"
        url = uri("https://maven.cleanroommc.com")
    }
    mavenCentral()
    mavenLocal() // Must be last for caching to work
}
dependencies {
    val lwjglVersion = "3.4.1"
    val lombokVersion = "1.18.42"
    val asmVersion = "9.9.1"

    compileOnly("com.cleanroommc:sponge-mixin:0.20.12+mixin.0.8.7")
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    compileOnly("org.apache.logging.log4j:log4j-api:2.0-beta9")

    implementation("org.joml:joml:1.10.5")
    "contain"("org.joml:joml:1.10.5")
    implementation("it.unimi.dsi:fastutil:7.1.0")
    "contain"("it.unimi.dsi:fastutil:7.1.0")
    compileOnly("org.ow2.asm:asm:$asmVersion")
    compileOnly("org.ow2.asm:asm-commons:$asmVersion")
    compileOnly("org.ow2.asm:asm-tree:$asmVersion")
    compileOnly("org.ow2.asm:asm-util:$asmVersion")
    implementation("com.google.code.gson:gson:2.10.1")

    "modImplementation"("zone.rong:mixinbooter:10.5")
    "modCompileOnly"("maven.modrinth:fluidlogged-api:3.0.6")

    compileOnly("org.lwjgl:lwjgl:$lwjglVersion")
    compileOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    if (propertyBool("enable_lwjglx")) {
        compileOnly("com.cleanroommc:lwjglx:1.0.0")
    }
    if (propertyBool("enable_junit_testing")) {
        testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    runtimeOnly("curse.maven:catalogue-vintage-1465746:7872634")
    // Example - Dependency descriptor:
    // 'com.google.code.gson:gson:2.8.6' << group: com.google.code.gson, name:gson, version:2.8.6
    // 'group:name:version:classifier' where classifier is optional

    // Example - CurseMaven dependencies:
    // 'curse.maven:had-enough-items-557549:4543375' << had-enough-items = project slug, 557549 = project id, 4543375 = file id
    // Full documentation: https://cursemaven.com/

    // Example - Modrinth dependencies:
    // 'maven.modrinth:jei:4.16.1.1000' << jei = project name, 4.16.1.1000 = file version
    // Full documentation: https://docs.modrinth.com/docs/tutorials/maven/

    // Common dependency types (configuration):
    // implementation = dependency available at both compile time and runtime
    // runtimeOnly = runtime dependency
    // compileOnly = compile time dependency
    // annotationProcessor = annotation processing dependencies
    // contain = bundle dependency jars into final artifact, will extract them in mod loading. Can be used it as jar-in-jar
    // shadow = bundle dependencies into shadow output artifact (relocation configurable in shadowJar task)
    // modImplementation = mod dependency available at both compile time and runtime
    // modCompileOnly = mod dependency available only at compile time

    // Transitive dependencies:
    // (Dependencies that your dependency depends on)
    // If you wish to exclude transitive dependencies in the described dependencies
    // Use a closure as such:
    // implementation ('com.google.code.gson:gson:2.8.6') {
    //     transitive = false
    // }
}
