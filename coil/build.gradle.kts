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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        // 下限由上游锁定：Coil 3 用 JVM 11 编译，且 ImageView.load 是 inline 函数，
        // 本模块降到 1.8 会直接报 "Cannot inline bytecode built with JVM target 11"
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
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
