import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    `kotlin-dsl`
    id("dev.clojurephant.clojure") version "0.7.0"
    id("org.jetbrains.intellij") version "1.13.3"
    id("org.jetbrains.changelog") version "1.3.1"
    id("org.jetbrains.grammarkit") version "2022.3.1"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "Clojars"
        url = uri("https://repo.clojars.org")
    }
}

dependencies {
    // implementation ("org.clojure:clojure:1.11.1")
    // https://clojure.atlassian.net/browse/ASYNC-248
    implementation ("org.clojure:core.async:1.5.648")
    implementation ("com.github.clojure-lsp:clojure-lsp-standalone:2023.07.01-22.35.41") {
        exclude("org.clojure", "core.async")
    }
    implementation ("com.rpl:proxy-plus:0.0.9")
    // TODO Stop using clojure-kit and write own gramar for Clojure lang
    // implementation(files("libs/clojure-kit-2020-3.1-lib.jar"))
    implementation ("markdown-clj:markdown-clj:1.11.4")
    // devDeps
    implementation ("nrepl:nrepl:1.0.0")
}

sourceSets {
    main {
        java.srcDirs("src/main", "src/gen")
        resources.srcDirs("resources")
    }
    test {
        java.srcDirs("tests")
    }
}

// Useful to override another IC platforms from env
val platformVersion = System.getenv("PLATFORM_VERSION") ?: properties("platformVersion")
val platformPlugins = System.getenv("PLATFORM_PLUGINS") ?: properties("platformPlugins")

intellij {
    pluginName.set(properties("pluginName"))
    version.set(platformVersion)
    type.set(properties("platformType"))
    plugins.set(platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty))
    updateSinceUntilBuild.set(false)
}

changelog {
    version.set(properties("pluginVersion"))
    groups.set(emptyList())
}

tasks.register("classpath") {
    val classpath = project.configurations.getByName("runtimeClasspath").files
    val clojureClasspath = clojure.builds.named("main").get().sourceRoots.files
    println(clojureClasspath.plus(classpath).joinToString(":"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = properties("javaVersion")
        kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            projectDir.resolve("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(provider {
            changelog.run {
                getOrNull(properties("pluginVersion")) ?: getLatest()
            }.toHTML()
        })
    }

    runPluginVerifier {
        ideVersions.set(properties("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty))
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("JETBRAINS_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
    }

    buildSearchableOptions {
        enabled = false
    }

    generateParser {
        sourceFile.set(file("src/main/gramar/clojure.bnf"))
        targetRoot.set("src/gen")
        pathToParser.set("com/github/clojure_lsp/intellij/language/parser/ClojureParser.java")
        pathToPsiRoot.set("com/github/clojure_lsp/intellij/language/psi")
        purgeOldFiles.set(true)
    }
}

grammarKit {
  jflexRelease.set("1.7.0-1")
  grammarKitRelease.set("2021.1.2")
  intellijRelease.set("203.7717.81")
}

tasks.register<DefaultTask>("foo") {
    doLast {
        println(sourceSets.main.get().compileClasspath)
    }
}

clojure.builds.named("main") {
    classpath.from(sourceSets.main.get().runtimeClasspath.asPath)
    checkAll()
    aotAll()
    reflection.set("fail")
}
