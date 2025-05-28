import org.jetbrains.kotlin.gradle.internal.config.LanguageFeature

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    compileSdk = 35
    namespace = "com.sec.sesl.tester"

    defaultConfig {
        applicationId = "com.tribalfs.oneuisample"
        vectorDrawables { useSupportLibrary = true }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    lint { baseline = file("lint-baseline.xml") }

    kotlinOptions{ jvmTarget = "21" }
}

dependencies {
    // sesl7 modules
    implementation(libs.bundles.sesl.androidx)
    implementation(libs.sesl.material)

    implementation(project(":oneui-design"))
    // implementation(libs.oneuiDesign)

    implementation(libs.oneuiIcons)

    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activityKtx)

    implementation(libs.bundles.androidx.datastore)
    implementation(libs.bundles.androidx.navigation)
    implementation(libs.bundles.hilt)
    ksp(libs.bundles.hilt.compilers)
}

kotlin {
    sourceSets.all {
        languageSettings.enableLanguageFeature(LanguageFeature.ExplicitBackingFields.name)
        languageSettings.enableLanguageFeature(LanguageFeature.WhenGuards.name)
    }
}

tasks.register("generateReleaseApk", Copy::class.java) {
    dependsOn(tasks.assemble)
    from(layout.buildDirectory.dir("outputs/apk/release"))
    into("$rootDir/sample-app/release/")
}

