import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.rikka.refine) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

apply(from = "manifest.gradle")


// Load GitHub credentials from properties file, gradle properties, or environment variables
fun getGithubProperty(key: String): String {
    val githubProperties = Properties().apply {
        val file = rootProject.file("github.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }
    return githubProperties.getProperty(key)
        ?: rootProject.findProperty(key)?.toString()
        ?: System.getenv(key.uppercase())
        ?: throw GradleException("GitHub $key not found")
}

val githubUsername = getGithubProperty("githubUsername")
val githubAccessToken = getGithubProperty("githubAccessToken")

allprojects {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
        maven {
            url = uri("https://maven.pkg.github.com/tribalfs/sesl-androidx")
            credentials {
                username = githubUsername
                password = githubAccessToken
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/tribalfs/sesl-material-components-android")
            credentials {
                username = githubUsername
                password = githubAccessToken
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/tribalfs/oneui-design")
            credentials {
                username = githubUsername
                password = githubAccessToken
            }
        }
    }
}


subprojects {
    plugins.withId("com.android.base") {
        plugins.apply("dev.rikka.tools.refine")
    }

    val group = "io.github.tribalfs"

    plugins.whenPluginAdded {
        val isAndroidLibrary = javaClass.name == "com.android.build.gradle.LibraryPlugin"
        val isAndroidApp = javaClass.name == "com.android.build.gradle.AppPlugin"

        if (isAndroidLibrary || isAndroidApp) {
            val artifact = project.name
            val versionInfo =
                rootProject.extensions.extraProperties.get("versions_metadata") as? Map<String, List<Any>>
            val artifactVersionInfo = versionInfo?.get(artifact)

            if (artifactVersionInfo == null) {
                throw GradleException("No version info found for module: $artifact")
            }

            val designVersion = versionInfo["oneui-design"]?.get(0).toString()

            extensions.findByType(BaseExtension::class.java)?.apply {
                defaultConfig.versionName = artifactVersionInfo[0].toString()
                compileSdkVersion((artifactVersionInfo[2] as Number).toInt())
                defaultConfig.minSdk = (artifactVersionInfo[1] as Number).toInt()
                defaultConfig.targetSdk = (artifactVersionInfo[2] as Number).toInt()
                buildFeatures.buildConfig = true
                defaultConfig.versionCode = 1
                
                when (this) {
                    is AppExtension -> {
                        defaultConfig.buildConfigField(
                            "String",
                            "ONEUI_DESIGN_VERSION",
                            "\"$designVersion\""
                        )
                    }

                    is LibraryExtension -> {

                        publishing {
                            singleVariant("release") {
                                withSourcesJar()
                                withJavadocJar()
                            }
                        }
                    }
                }

            }

            afterEvaluate {
                if (!plugins.hasPlugin("maven-publish")) return@afterEvaluate

                if (artifact == "oneui-design" || artifact == "oneui-icons") {
                    file("${rootProject.projectDir}/README.md").apply {
                        if (exists()) {
                            val readmeContent = readText()
                            val newVersionString = "$group:$artifact:$designVersion"
                            val oneuiVersion = "oneui\\d+".toRegex().find(designVersion)?.value ?: ""
                            val pattern =
                                "io\\.github\\.tribalfs:$artifact:\\S+$oneuiVersion".toRegex()

                            writeText(readmeContent.replace(pattern, newVersionString))
                            println("Updated README.md with version: $newVersionString")
                        }
                    }
                }

                extensions.findByType(PublishingExtension::class.java)?.apply {
                    publications {
                        create("mavenJava", MavenPublication::class.java) {
                            version = designVersion
                            groupId = group
                            artifactId = artifact
                            afterEvaluate { from(components.findByName("release")) }

                            pom {
                                name.set(artifact)
                                url.set("https://github.com/tribalfs/oneui-design")
                                developers {
                                    developer {
                                        id.set("tribalfs")
                                        name.set("Tribalfs")
                                        email.set("tribalfs@gmail.com")
                                        url.set("https://github.com/tribalfs")
                                    }
                                }
                                licenses {
                                    license {
                                        name.set("MIT License")
                                        url.set("https://github.com/tribalfs/oneui-design/blob/main/LICENSE")
                                        distribution.set("repo")
                                    }
                                }
                            }
                        }
                    }
                    repositories {
                        maven {
                            name = "GitHubPackages"
                            url = uri("https://maven.pkg.github.com/tribalfs/oneui-design")
                            credentials {
                                username = githubUsername
                                password = githubAccessToken
                            }
                        }
                    }
                }

            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}