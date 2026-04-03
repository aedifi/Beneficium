plugins {
    java
}

group = "aedifi"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":aedi-api"))
    implementation("org.luaj:luaj-jse:3.0.1")
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    testCompileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    testRuntimeOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(":aedi-api").sourceSets.main.get().output)
    from(
        configurations.runtimeClasspath.get()
            .filter { it.name.startsWith("luaj") && it.name.endsWith(".jar") }
            .map { zipTree(it) }
    )
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
