import org.jetbrains.kotlin.gradle.internal.config.LanguageFeature

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    compileSdk = 36
    namespace = "com.sec.sesl.tester"

    defaultConfig {
        applicationId = "com.tribalfs.oneuisample"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("androiddebug") {
            val keystorePath = System.getProperty("user.home") + "/.android/debug.keystore"
            storeFile = file(keystorePath)
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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
            signingConfig = signingConfigs.getByName("androiddebug")
        }
    }

    lint { baseline = file("lint-baseline.xml") }
}

dependencies {
    //implementation(project(":oneui-design"))
    implementation(libs.oneuiDesign)

    // Since v0.7.6+oneui7, it's not anymore required to explicitly declare
    // sesl-androidx, sesl-material, or androidx navigation dependencies,
    // unless you need versions newer than those bundled with this design library.
    // However, GitHub authentication is still required to resolve the sesl* modules.
    implementation(libs.bundles.sesl.androidx)
    implementation(libs.sesl.material)
    implementation(libs.bundles.androidx.navigation)

    implementation(libs.oneuiIcons)

    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activityKtx)

    implementation(libs.bundles.androidx.datastore)
    implementation(libs.bundles.hilt)
    ksp(libs.bundles.hilt.compilers)
    implementation(libs.lottie)
}

tasks.register("generateReleaseApk", Copy::class.java) {
    dependsOn(tasks.assemble)
    from(layout.buildDirectory.dir("outputs/apk/release"))
    into("$rootDir/sample-app/release/")
}

