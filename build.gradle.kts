import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

// 发布坐标集中在此处维护，模块内不再各自声明
val libraryGroup: String = providers.gradleProperty("libwrap.group").getOrElse("io.github.git-a-live")
val libraryVersion: String = providers.gradleProperty("libwrap.version").getOrElse("0.1.2")
val projectHomepage = "https://github.com/Git-a-Live/OpenLibsExtensions"

/**
 * 为单个模块注册 maven publication。
 *
 * @param componentName AGP 的软件组件固定为 release；Kotlin/JVM 模块为 java
 */
fun Project.registerReleasePublication(componentName: String, homepage: String) {
    apply(plugin = "maven-publish")

    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension> {
            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }
    }

    // 软件组件由 AGP / java 插件在评估期后段创建，必须等评估完成再引用
    afterEvaluate {
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("release") {
                    from(components[componentName])
                    artifactId = project.name
                    pom {
                        name.set("${project.name} · OpenLibsExtensions")
                        description.set("OpenLibsExtensions 的 ${project.name} 模块，仓库地址 $homepage")
                        this.url.set(homepage)
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }
                        developers {
                            developer {
                                id.set("Git-a-Live")
                                name.set("Git-a-Live")
                            }
                        }
                        scm {
                            this.url.set(homepage)
                            connection.set("scm:git:$homepage.git")
                            developerConnection.set("scm:git:$homepage.git")
                        }
                    }
                }
            }

            // 远端仓库由参数驱动：不传 -PpublishRepoUrl 时只发布到本地 mavenLocal
            val repoUrl = providers.gradleProperty("publishRepoUrl").orNull
            if (!repoUrl.isNullOrBlank()) {
                repositories {
                    maven {
                        name = "remote"
                        setUrl(repoUrl)
                        credentials {
                            username = providers.gradleProperty("publishRepoUser").orNull
                                ?: System.getenv("MAVEN_USER")
                            password = providers.gradleProperty("publishRepoPassword").orNull
                                ?: System.getenv("MAVEN_PASSWORD")
                        }
                    }
                }
            }
        }

        // GPG 签名只在提供密钥时启用：本地安装与 JitPack 构建都不需要签名
        val signingKey = providers.gradleProperty("signingKey").orNull
            ?: System.getenv("SIGNING_KEY")
        if (!signingKey.isNullOrBlank()) {
            val signingPassword = providers.gradleProperty("signingPassword").orNull
                ?: System.getenv("SIGNING_PASSWORD")
            apply(plugin = "signing")
            extensions.configure<SigningExtension> {
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(extensions.getByType<PublishingExtension>().publications)
            }
        }
    }
}

subprojects {
    group = libraryGroup
    version = libraryVersion

    // 用 withId 而非 hasPlugin：根脚本执行时子模块尚未应用插件
    plugins.withId("com.android.library") {
        registerReleasePublication("release", projectHomepage)
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        registerReleasePublication("java", projectHomepage)
    }
}
