import org.jetbrains.changelog.markdownToHTML
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)
fun prop(name: String): String {
    return properties(name).get()
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("dev.clojurephant.clojure") version "0.7.0"
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.changelog") version "1.3.1"
    id("org.jetbrains.grammarkit") version "2021.2.2"
}

group = prop("pluginGroup")
version = prop("pluginVersion")

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
    implementation ("com.github.ericdallo:clj4intellij:0.8.0")
    implementation ("seesaw:seesaw:1.5.0")
    implementation ("camel-snake-kebab:camel-snake-kebab:0.4.3")
    implementation ("com.rpl:proxy-plus:0.0.9")
    implementation ("dev.weavejester:cljfmt:0.13.0")
    implementation ("com.github.clojure-lsp:clojure-lsp:2025.01.22-23.28.23")
    implementation ("nrepl:nrepl:1.3.1")

    testImplementation("junit:junit:latest.release")
    testImplementation("org.junit.platform:junit-platform-launcher:latest.release")
    testRuntimeOnly ("dev.clojurephant:jovial:0.4.2")
}

sourceSets {
    main {
        java.srcDirs("src/main", "src/gen")
        if (project.gradle.startParameter.taskNames.contains("buildPlugin") ||
            project.gradle.startParameter.taskNames.contains("clojureRepl") ||
            project.gradle.startParameter.taskNames.contains("runIde")) {
            resources.srcDirs("src/main/dev-resources")
        }
    }
    test {
        java.srcDirs("tests")
    }
}

intellij {
    pluginName.set(prop("pluginName"))
    version.set(prop("platformVersion"))
    type.set(prop("platformType"))
    updateSinceUntilBuild.set(false)

    val platformPlugins =  ArrayList<Any>()
    val localLsp4ij = file("../lsp4ij/build/idea-sandbox/plugins/LSP4IJ").absoluteFile
    if (localLsp4ij.isDirectory) {
        // In case Gradle fails to build because it can't find some missing jar, try deleting
        // ~/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.idea/unzipped.com.jetbrains.plugins/com.redhat.devtools.lsp4ij*
        platformPlugins.add(localLsp4ij)
    } else {
        // When running on CI or when there's no local lsp4ij
        val latestLsp4ijNightlyVersion = fetchLatestLsp4ijNightlyVersion()
        platformPlugins.add("com.redhat.devtools.lsp4ij:$latestLsp4ijNightlyVersion@nightly")
    }
    //Uses `platformPlugins` property from the gradle.properties file.
    platformPlugins.addAll(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }.get())
    plugins.set(platformPlugins)
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
        gradleVersion = prop("gradleVersion")
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
                getOrNull(prop("pluginVersion")) ?: getLatest()
            }.toHTML()
        })
    }

    runPluginVerifier {
        ideVersions.set(prop("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty))
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    test {
        systemProperty("idea.mimic.jar.url.connection", "true")
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

    clojureRepl {
        dependsOn("compileClojure")
        classpath.from(sourceSets.main.get().runtimeClasspath
                       + file("build/classes/kotlin/main")
                       + file("build/clojure/main")
        )
        // doFirst {
        //     println(classpath.asPath)
        // }
        forkOptions.jvmArgs = listOf("--add-opens=java.desktop/java.awt=ALL-UNNAMED",
                                     "--add-opens=java.desktop/java.awt.event=ALL-UNNAMED",
                                     "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
                                     "--add-opens=java.desktop/sun.font=ALL-UNNAMED",
                                     "--add-opens=java.base/java.lang=ALL-UNNAMED",
                                     "-Djava.system.class.loader=com.intellij.util.lang.PathClassLoader",
                                     "-Didea.mimic.jar.url.connection=true",
                                     "-Didea.force.use.core.classloader=true"
        )
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

grammarKit {
  jflexRelease.set("1.7.0-1")
  grammarKitRelease.set("2021.1.2")
  intellijRelease.set("203.7717.81")
}

clojure.builds.named("main") {
    classpath.from(sourceSets.main.get().runtimeClasspath.asPath + "build/classes/kotlin/main")
    checkAll()
    aotAll()
    reflection.set("fail")
}

fun fetchLatestLsp4ijNightlyVersion(): String {
    val client = HttpClient.newBuilder().build();
    var onlineVersion = ""
    try {
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI("https://plugins.jetbrains.com/api/plugins/23257/updates?channel=nightly&size=1"))
            .GET()
            .timeout(Duration.of(10, ChronoUnit.SECONDS))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString());
        val pattern = Pattern.compile("\"version\":\"([^\"]+)\"")
        val matcher = pattern.matcher(response.body())
        if (matcher.find()) {
            onlineVersion = matcher.group(1)
            println("Latest approved nightly build: $onlineVersion")
        }
    } catch (e:Exception) {
        println("Failed to fetch LSP4IJ nightly build version: ${e.message}")
    }

    val minVersion = "0.0.1-20231213-012910"
    return if (minVersion < onlineVersion) onlineVersion else minVersion
}
