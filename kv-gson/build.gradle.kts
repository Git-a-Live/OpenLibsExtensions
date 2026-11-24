plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.libwrap.kv.gson"
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
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
    explicitApi()
}

dependencies {
    api(project(":kv"))
    // Gson 出现在公开构造器参数上（允许宿主注入自定义实例）
    api(libs.gson)
    testImplementation(libs.junit)
}
