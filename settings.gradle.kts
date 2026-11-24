pluginManagement {
    // JitPack 构建机位于海外，阿里云镜像反而增加延迟；本地开发走镜像才能避免 Google Maven 超时
    val onJitPackBuild = System.getenv("JITPACK") == "true"
    repositories {
        if (!onJitPackBuild) {
            maven("https://maven.aliyun.com/repository/gradle-plugin")
            maven("https://maven.aliyun.com/repository/public")
            maven("https://maven.aliyun.com/repository/google")
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    val onJitPackBuild = System.getenv("JITPACK") == "true"
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (!onJitPackBuild) {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "OpenLibsExtensions"

include(":coil")
include(":lottie")
include(":gson")
include(":moshi")
include(":zxing")
include(":kv")
include(":kv-gson")
include(":demo")
