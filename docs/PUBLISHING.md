# 发布与维护

面向仓库维护者。使用者只需要看根目录 [README](../README.md) 的「接入」一节。

## 1. 坐标

group 与 version 集中在根 `build.gradle.kts` 读取，默认值 `io.github.git-a-live` / `0.1.0`。

三种改法，优先级从低到高：

```bash
# 1) gradle.properties（改一次长期生效）
libwrap.group=io.github.git-a-live
libwrap.version=0.1.0

# 2) 命令行参数（单次生效）
./gradlew publishToMavenLocal -Plibwrap.group=io.github.git-a-live -Plibwrap.version=0.1.0

# 3) 发布到远端仓库时额外指定地址与凭据
./gradlew publish -PpublishRepoUrl=https://... -PpublishRepoUser=xxx -PpublishRepoPassword=xxx
```

不传 `publishRepoUrl` 时不会添加任何远端仓库，`publishToMavenLocal` 只写本机 `~/.m2`。

模块的 `artifactId` 自动取目录名（`coil` / `gson` / `lottie` / `zxing`），不单独配置。

## 2. 发布前自检

```bash
./gradlew test                      # 60 个用例必须全绿
./gradlew publishToMavenLocal       # 四个模块各产出 aar/jar + sources + pom
```

检查产物形态：

```bash
ls ~/.m2/repository/io/github/git-a-live/coil/0.1.0/
# coil-0.1.0.aar / coil-0.1.0-sources.jar / coil-0.1.0.pom / coil-0.1.0.module

unzip -p coil/build/outputs/aar/coil-release.aar \
  META-INF/com/android/build/gradle/aar-metadata.properties
# minCompileSdk=37 ← 这个值会强制宿主的 compileSdk 下限
```

`minCompileSdk` 来自模块的 `compileSdk`，也受依赖 AAR 的约束（Coil 3.6.3 自身要求 37）。降低 `compileSdk` 前先确认依赖链允许。

## 3. tag 与 Release

```bash
git tag v0.1.0
git push origin v0.1.0
```

`.github/workflows/release.yml` 会在 tag 上执行：

1. `./gradlew test`（失败则中断，不创建 Release）
2. `gh release create <tag> --generate-notes`

GitHub Release 是 JitPack 的构建入口，没有 Release 就只有 `main-SNAPSHOT` 可用。

`ci.yml` 在 push / PR 上执行 `test` → `:demo:assembleDebug` → `publishToMavenLocal`，最后一步是为了让发布配置的腐化在 PR 阶段就暴露。

## 4. JitPack

零配置：JitPack 读取仓库里的 `jitpack.yml`，用 `openjdk21` 执行

```
./gradlew clean publishToMavenLocal --stacktrace
```

然后把 `~/.m2` 里的产物按 `com.github.git-a-live.openlibsextensions:模块名:版本` 的坐标对外提供。

几个需要知道的机制：

- **按需构建**：第一次有人请求某个版本时才构建，通常 1~3 分钟。日志：`https://jitpack.io/com/github/git-a-live/openlibsextensions/版本/build.log`
- **缓存**：同一版本构建成功后会被缓存，客户端需要 `--refresh-dependencies` 才能拿到重建结果
- **不可变**：发布后 7 天产物冻结，之后的修复必须发新 tag
- **多模块**：每个模块单独一个 artifactId，取的是 `artifactId` 而不是目录名（本项目两者一致）

`settings.gradle.kts` 里有一个分支：检测到 `JITPACK=true` 时跳过阿里云镜像。JitPack 构建机在海外，镜像反而拖慢；本地开发没有这个环境变量，仍然走镜像。

## 5. Maven Central（未接入）

如果将来要发到 Maven Central，还需要：

1. Sonatype 账号与命名空间验证（通常用 GitHub Pages 放一个校验文件）
2. GPG 密钥对，私钥通过 CI secret 注入
3. 上传到 Central Portal，而不是直接 `publish` 到某个 Maven 仓库

构建脚本已预留参数：

| 参数 / 环境变量 | 作用 |
|---|---|
| `-PpublishRepoUrl` | 添加名为 `remote` 的 Maven 仓库 |
| `-PpublishRepoUser` / `MAVEN_USER` | 仓库用户名 |
| `-PpublishRepoPassword` / `MAVEN_PASSWORD` | 仓库密码 |
| `-PsigningKey` / `SIGNING_KEY` | ASCII-armored 私钥，提供后自动启用 GPG 签名 |
| `-PsigningPassword` / `SIGNING_PASSWORD` | 私钥口令 |

签名是**条件启用**的：没提供 `signingKey` 就不会应用 `signing` 插件，因此本地安装与 JitPack 构建都不受签名影响。

目前没有 Central Portal 的上传流水线——写了也无法在本地验证，不如留白。

## 6. 版本策略

本库按语义化版本推进，但 Kotlin 库有一个容易踩的点：

**给 data class / 构造器加带默认值的参数，对 Kotlin 调用方是二进制破坏。** Kotlin 编译器为默认参数生成合成的 `$default` 构造器，参数个数变化后，用旧版本编译的宿主会在运行时找不到该方法。

受影响的公开类型：`BlurBand`、`GradientBlurOptions`、`QrCodeOptions`。

处理方式：

- 新增可选能力优先用**新函数/新重载**，而不是改现有构造器
- 确实需要改构造器时升 minor 版本并在 CHANGELOG 标注，提示使用者重新编译
- `@JvmOverloads` 只能保护 Java 调用方，保护不了 Kotlin 调用方
