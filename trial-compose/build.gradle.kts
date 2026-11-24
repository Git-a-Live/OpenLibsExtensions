plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.libwrap.trial.compose"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
    }

    buildFeatures {
        compose = true
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
    // TrialRange 与 TrialSeekBarStyle 出现在公开签名上
    api(project(":trial"))
    implementation(platform(libs.androidx.compose.bom))
    // Modifier、@Composable 出现在公开签名上
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    testImplementation(libs.junit)
}
