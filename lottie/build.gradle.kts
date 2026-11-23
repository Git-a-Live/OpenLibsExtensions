plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.libwrap.lottie"
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

kotlin {
    // 公开符号必须显式声明可见性与返回类型，避免默认可见性把内部实现泄漏成 ABI
    explicitApi()
}

dependencies {
    api(libs.lottie)
    // 公开签名里出现 @RawRes，注解类需对宿主可见
    api(libs.androidx.annotation)
    testImplementation(libs.junit)
}
