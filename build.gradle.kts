import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("dev.clojurephant.clojure") version "0.7.0"
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.changelog") version "1.3.1"
    id("org.jetbrains.grammarkit") version "2021.2.2"
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
    implementation ("org.clojure:clojure:1.12.0")
    implementation ("com.github.ericdallo:clj4intellij:0.5.4")
    implementation ("seesaw:seesaw:1.5.0")
    implementation ("camel-snake-kebab:camel-snake-kebab:0.4.3")
    implementation ("babashka:process:0.5.18")
    implementation ("com.github.clojure-lsp:lsp4clj:1.9.3")
    implementation ("org.clojure:core.async:1.5.648") {
        because("issue https://clojure.atlassian.net/browse/ASYNC-248")
    }
    implementation ("com.github.clojure-lsp:clojure-lsp:2024.03.01-11.37.51") {
        exclude("org.clojure", "core.async")
    }
    implementation ("com.rpl:proxy-plus:0.0.9")
    implementation ("markdown-clj:markdown-clj:1.12.1") {
        exclude("clj-commons", "clj-yaml")
    }
    implementation ("clj-commons:clj-yaml:1.0.29")
}

sourceSets {
    main {
        java.srcDirs("src/main", "src/gen")
        if (project.gradle.startParameter.taskNames.contains("buildPlugin") ||
            project.gradle.startParameter.taskNames.contains("runIde")) {
            resources.srcDirs("src/main/dev-resources")
        }
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

java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks.register("classpath") {
    doFirst {
        println(sourceSets["main"].compileClasspath.asPath)
    }
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
            apiVersion = "1.9"
            languageVersion = "1.9"
            freeCompilerArgs = listOf("-Xjvm-default=all")
        }
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
        channels.set(listOf("default"))
    }

    buildSearchableOptions {
        enabled = false
    }

    generateParser {
        source.set("src/main/gramar/clojure.bnf")
        targetRoot.set("src/gen")
        pathToParser.set("com/github/clojure_lsp/intellij/language/parser/ClojureParser.java")
        pathToPsiRoot.set("com/github/clojure_lsp/intellij/language/psi")
        purgeOldFiles.set(true)
    }

    generateLexer {
        source.set("src/main/gramar/_ClojureLexer.flex")
        targetDir.set("src/gen/com/github/clojure_lsp/intellij/language/parser")
        targetClass.set("_ClojureLexer")
    }
}

grammarKit {
  jflexRelease.set("1.7.0-1")
  grammarKitRelease.set("2021.1.2")
  intellijRelease.set("203.7717.81")
}

clojure.builds.named("main") {
    classpath.from(sourceSets.main.get().runtimeClasspath.asPath)
    checkAll()
    aotAll()
    reflection.set("fail")
}
