plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.tribalfs.stub"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        minSdk = 21
    }
}

dependencies {
    annotationProcessor(libs.rikka.refineAnnotationProcessor)
    compileOnly(libs.rikka.refineAnnotation)
}
