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
    dokkaSourceSets.main {
        sourceLink {
            localDirectory.set(file("src/main/java"))
            remoteUrl("https://github.com/oneui-design")
            remoteLineSuffix.set("#L")
        }

        pluginsConfiguration.html {
            /*customStyleSheets.from("styles.css")
            customAssets.from("logo.png")*/
            footerMessage.set("(c) Tribalfs")
        }
    }
}