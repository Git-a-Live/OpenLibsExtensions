plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.libwrap.coil"
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
    api(libs.coil)
    // 公开签名里出现 @DrawableRes / @RequiresApi，注解类需对宿主可见
    api(libs.androidx.annotation)
    testImplementation(libs.junit)
}
