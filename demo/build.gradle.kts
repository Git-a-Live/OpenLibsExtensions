plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.libwrap.demo"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.libwrap.demo"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":coil"))
    implementation(project(":lottie"))
    implementation(project(":gson"))
    implementation(project(":zxing"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)

    // Compose 演示页：试听区间进度条
    implementation(project(":trial"))
    implementation(project(":trial-compose"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
}
