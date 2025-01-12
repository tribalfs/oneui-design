plugins {
    id ("com.android.library")
}

android {
    namespace = "io.github.tribalfs.stub"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }
}

dependencies {
    annotationProcessor("dev.rikka.tools.refine:annotation-processor:4.4.0")
    compileOnly("dev.rikka.tools.refine:annotation:4.4.0")
}
