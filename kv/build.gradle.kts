plugins {
    alias(libs.plugins.android.library)
    // 测试里的 @Serializable 数据类需要序列化插件
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.libwrap.kv"
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
    explicitApi()
}

dependencies {
    // 写入函数是内联的，序列化运行时必须在编译期可见，因此用 api
    api(libs.kotlinx.serialization.json)
    // MMKV 只出现在实现里，不暴露给宿主，避免宿主的 MMKV 版本与本模块耦合
    implementation(libs.mmkv)
    // DataStore 出现在公开的 init(DataStore<Preferences>) 签名上，需要 api
    api(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
}
