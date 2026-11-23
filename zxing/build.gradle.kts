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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    compilerOptions {
        // 取 Android 生态的最低公共标准：低于 1.8 会让宿主无法内联本模块的公开 inline 函数
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
    // 公开符号必须显式声明可见性与返回类型，避免默认可见性把内部实现泄漏成 ABI
    explicitApi()
}

dependencies {
    // 只用到 android.graphics 与 zxing，不依赖任何 androidx 组件
    api(libs.zxing.core)
    testImplementation(libs.junit)
}
