plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.tribalfs.stub"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }
}

dependencies {
    annotationProcessor(libs.rikka.refineAnnotationProcessor)
    compileOnly(libs.rikka.refineAnnotation)
}
