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

kotlin {
    // 公开符号必须显式声明可见性与返回类型：漏写 public 会被当成编译错误，
    // 避免内部实现因为默认可见性意外成为 ABI 的一部分
    explicitApi()
}

dependencies {
    api(libs.coil)
    // 公开签名里出现 @DrawableRes / @RequiresApi，注解类需对宿主可见
    api(libs.androidx.annotation)
    testImplementation(libs.junit)
}
