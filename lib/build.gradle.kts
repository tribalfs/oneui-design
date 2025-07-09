import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
}

android {
    namespace = "dev.oneuiproject.oneui.design"
    defaultConfig {
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildTypes.all { consumerProguardFiles("consumer-rules.pro") }
}

dependencies {
    implementation(libs.bundles.sesl.androidx)
    implementation(libs.sesl.material)

    api(libs.androidx.annotation)

    implementation(libs.zxing.core)
    implementation(libs.lottie)
    implementation(libs.rikka.refineRuntime)

    compileOnly(project(":stub"))

    implementation(libs.bundles.androidx.navigation)
    lintPublish(project(":lint"))
}


tasks.register("releaseJavadocJar", Jar::class.java) {
    dependsOn(tasks.dokkaGeneratePublicationJavadoc)
    archiveClassifier.set("javadoc")
    from(layout.buildDirectory.dir("dokka/javadoc"))
}

val dokkaHtmlJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGeneratePublicationHtml)
    archiveClassifier.set("html-docs")
    from(layout.buildDirectory.dir("dokka/html"))
}

dokka {
    moduleName.set(rootProject.name)
    dokkaPublications.html {
        suppressObviousFunctions.set(true)
        failOnWarning.set(false)
        suppressInheritedMembers.set(true)
    }

    dokkaSourceSets.main {
        sourceRoots.from(file("src"))
        skipDeprecated.set(true)
        displayName.set(name)
        reportUndocumented.set(true)
        documentedVisibilities.set(setOf(VisibilityModifier.Public))

        sourceLink {
            localDirectory.set(projectDir.resolve("src"))
            remoteUrl("https://github.com/tribalfs/oneui-design/blob/main/lib/src")
            remoteLineSuffix.set("#L")
        }

        externalDocumentationLinks{
            register("sesl.androidx") {
                url("https://tribalfs.github.io/sesl-androidx/")
                packageListUrl("https://tribalfs.github.io/sesl-androidx/package-list")
            }
            register("sesl.material") {
                url("https://tribalfs.github.io/sesl-material-components-android/")
                packageListUrl("https://tribalfs.github.io/sesl-material-components-android/-s-e-s-l%20-material/package-list")
            }
        }
    }
}