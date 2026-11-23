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
    // 只用到 android.graphics 与 zxing，不依赖任何 androidx 组件
    api(libs.zxing.core)
    testImplementation(libs.junit)
}
