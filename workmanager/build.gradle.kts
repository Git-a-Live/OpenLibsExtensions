plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.libwrap.workmanager"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    // 公开符号必须显式声明可见性与返回类型，避免默认可见性把内部实现泄漏成 ABI
    explicitApi()
}

dependencies {
    // WorkRequest 系列类型出现在公开签名上
    api(libs.androidx.work.runtime)
    api(libs.androidx.annotation)
    testImplementation(libs.junit)
}
