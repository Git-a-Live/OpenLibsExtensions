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

kotlin {
    // 公开符号必须显式声明可见性与返回类型，避免默认可见性把内部实现泄漏成 ABI
    explicitApi()
}

dependencies {
    // 只用到 android.graphics 与 zxing，不依赖任何 androidx 组件
    api(libs.zxing.core)
    testImplementation(libs.junit)
}
