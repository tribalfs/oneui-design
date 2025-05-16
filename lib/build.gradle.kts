plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "dev.oneuiproject.oneui.design"
    defaultConfig {
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes.all { consumerProguardFiles("consumer-rules.pro") }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions { jvmTarget = "21" }
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

configurations.all {
    // Exclude official android jetpack modules
    exclude(group = "androidx.core", module = "core")
    exclude(group = "androidx.core", module = "core-ktx")
    exclude(group = "androidx.customview", module = "customview")
    exclude(group = "androidx.coordinatorlayout", module = "coordinatorlayout")
    exclude(group = "androidx.drawerlayout", module = "drawerlayout")
    exclude(group = "androidx.viewpager2", module = "viewpager2")
    exclude(group = "androidx.viewpager", module = "viewpager")
    exclude(group = "androidx.appcompat", module = "appcompat")
    exclude(group = "androidx.fragment", module = "fragment")
    exclude(group = "androidx.preference", module = "preference")
    exclude(group = "androidx.recyclerview", module = "recyclerview")
    exclude(group = "androidx.slidingpanelayout", module = "slidingpanelayout")
    exclude(group = "androidx.swiperefreshlayout", module = "swiperefreshlayout")
    // Exclude official material components lib
    exclude(group = "com.google.android.material", module = "material")
}
