import org.jetbrains.kotlin.gradle.internal.config.LanguageFeature

plugins {
    id("kotlin")
}

dependencies {
    compileOnly(libs.androidLintApi)
    compileOnly(libs.kotlinStdlib)
}

kotlin {
    sourceSets.all {
        languageSettings.enableLanguageFeature(LanguageFeature.WhenGuards.name)
    }
}
