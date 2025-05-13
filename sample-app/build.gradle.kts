plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions { jvmTarget = "21" }

    lint { baseline = file("lint-baseline.xml") }
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

}

configurations.implementation {
    // Exclude official android jetpack modules
    exclude("androidx.core", "core")
    exclude("androidx.core", "core-ktx")
    exclude("androidx.customview", "customview")
    exclude("androidx.coordinatorlayout", "coordinatorlayout")
    exclude("androidx.drawerlayout", "drawerlayout")
    exclude("androidx.viewpager2", "viewpager2")
    exclude("androidx.viewpager", "viewpager")
    exclude("androidx.appcompat", "appcompat")
    exclude("androidx.fragment", "fragment")
    exclude("androidx.preference", "preference")
    exclude("androidx.recyclerview", "recyclerview")
    exclude("androidx.slidingpanelayout", "slidingpanelayout")
    exclude("androidx.swiperefreshlayout", "swiperefreshlayout")
    // Exclude official material components lib
    exclude("com.google.android.material", "material")
}

configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor(30, "seconds")
}
