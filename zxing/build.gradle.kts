plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.libwrap.zxing"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.zxing.core)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}
