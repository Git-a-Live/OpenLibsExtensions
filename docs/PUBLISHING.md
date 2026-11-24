# 发布与维护

面向仓库维护者。使用方文档见根目录 [README](../README.md)，贡献与构建说明见 [DEVELOPING](DEVELOPING.md)。

## 1. 坐标

group 与 version 在根 `build.gradle.kts` 中读取，默认值为 `io.github.git-a-live` / `0.1.4`，以该文件为准。

三种配置方式，优先级由低到高：

```bash
# 1) gradle.properties，长期生效
libwrap.group=io.github.git-a-live
libwrap.version=0.1.4

# 2) 命令行参数，单次生效
./gradlew publishToMavenLocal -Plibwrap.group=io.github.git-a-live -Plibwrap.version=0.1.4

# 3) 发布到远端仓库时附加地址与凭据
./gradlew publish -PpublishRepoUrl=https://... -PpublishRepoUser=xxx -PpublishRepoPassword=xxx
```

未传 `publishRepoUrl` 时不添加任何远端仓库，`publishToMavenLocal` 仅写入本机 `~/.m2`。

模块的 `artifactId` 取目录名（`coil` / `gson` / `moshi` / `lottie` / `zxing` / `kv` / `kv-gson` / `coroutines` / `workmanager`），不单独配置。

## 2. 发布前自检

```bash
./gradlew test                      # 111 个用例需全部通过
./gradlew publishToMavenLocal       # 九个模块各产出 aar/jar + sources + pom
```

产物形态检查：

```bash
ls ~/.m2/repository/io/github/git-a-live/coil/0.1.4/
# coil-0.1.4.aar / coil-0.1.4-sources.jar / coil-0.1.4.pom / coil-0.1.4.module

unzip -p coil/build/outputs/aar/coil-release.aar \
  META-INF/com/android/build/gradle/aar-metadata.properties
# minCompileSdk=37，该值决定宿主 compileSdk 的下限
```

`minCompileSdk` 来自模块的 `compileSdk`，同时受依赖 AAR 约束（Coil 3.6.3 自身要求 37）。调整 `compileSdk` 前需确认依赖链允许。

## 3. tag 与 Release

```bash
git tag v0.1.4
git push origin v0.1.4
```

`.github/workflows/release.yml` 在 tag 上执行：

1. `./gradlew test`，失败则中断，不创建 Release
2. `gh release create <tag> --generate-notes`

GitHub Release 是 JitPack 的构建入口。无 Release 时仅 `main-SNAPSHOT` 可用。

`ci.yml` 在 push 与 PR 上执行 `test`、`:demo:assembleDebug`、`publishToMavenLocal`，其中最后一步用于在 PR 阶段暴露发布配置的腐化。

## 4. JitPack

JitPack 读取仓库内的 `jitpack.yml`，以 `openjdk21` 执行

```
./gradlew clean publishToMavenLocal --stacktrace
```

随后将 `~/.m2` 中的产物按 `com.github.git-a-live.openlibsextensions:模块名:版本` 对外提供。

构建机制：

- **按需构建**：首次请求某版本时开始构建，通常 1~3 分钟。日志地址为 `https://jitpack.io/com/github/git-a-live/openlibsextensions/版本/build.log`
- **缓存**：构建成功后该版本被缓存，客户端需使用 `--refresh-dependencies` 获取重建结果
- **不可变**：发布 7 天后产物冻结，后续修复必须发布新 tag
- **多模块**：每个模块单独一个 artifactId

`settings.gradle.kts` 中按环境区分依赖源：检测到 `JITPACK=true` 时跳过阿里云镜像。JitPack 构建机位于海外，镜像会增加延迟；本地开发无该环境变量，仍走镜像。

## 5. Maven Central

当前未接入。发布到 Maven Central 还需：

1. Sonatype 账号与命名空间验证
2. GPG 密钥对，私钥通过 CI secret 注入
3. 通过 Central Portal 上传，而非直接 `publish` 到某个 Maven 仓库

构建脚本已预留参数：

| 参数 / 环境变量 | 作用 |
|---|---|
| `-PpublishRepoUrl` | 添加名为 `remote` 的 Maven 仓库 |
| `-PpublishRepoUser` / `MAVEN_USER` | 仓库用户名 |
| `-PpublishRepoPassword` / `MAVEN_PASSWORD` | 仓库密码 |
| `-PsigningKey` / `SIGNING_KEY` | ASCII-armored 私钥，提供后启用 GPG 签名 |
| `-PsigningPassword` / `SIGNING_PASSWORD` | 私钥口令 |

签名为条件启用：未提供 `signingKey` 时不应用 `signing` 插件，本地安装与 JitPack 构建均不受影响。

Central Portal 的上传流水线尚未实现。

## 6. 版本策略

版本号遵循语义化版本。Kotlin 库存在一处易被忽略的二进制兼容约束：

**为 data class 或构造器新增带默认值的参数，对 Kotlin 调用方构成二进制破坏。** Kotlin 编译器为默认参数生成合成的 `$default` 构造器，参数个数变化后，以旧版本编译的宿主在运行时会找不到该方法。

受影响的公开类型：`BlurBand`、`GradientBlurOptions`、`QrCodeOptions`。

处理方式：

- 新增可选能力优先采用新函数或新重载，不改动现有构造器
- 确需修改构造器时升 minor 版本，并在 CHANGELOG 标注需重新编译
- `@JvmOverloads` 仅保护 Java 调用方，对 Kotlin 调用方无效

## 7. 发布后验证

制品可下载不等于依赖可解析。JitPack 可能正常返回 POM，而 Gradle 侧因坐标或仓库过滤配置问题无法解析，因此每次发布后需以消费方视角验证一次。

第一步，确认制品存在：

```bash
BASE=https://jitpack.io/com/github/git-a-live/openlibsextensions
curl -s -o /dev/null -w "%{http_code}\n" $BASE/coil/v0.1.4/coil-v0.1.4.aar
```

第二步，以临时模块实际解析：

```kotlin
// 临时加入 settings.gradle.kts
include(":scratch")
```
```kotlin
// scratch/build.gradle.kts，android library，无源码
dependencies {
    implementation("com.github.git-a-live.openlibsextensions:coil:v0.1.4")
}
```
```bash
./gradlew :scratch:dependencies --configuration debugCompileClasspath --refresh-dependencies
```

输出中出现 `FAILED` 表示坐标或仓库过滤存在问题。验证完成后删除 `scratch/` 与 `include(":scratch")`。

注意事项：`includeGroup` 为精确匹配。多模块项目的实际 group 是 `com.github.用户.仓库名`（三段），若配置写成两段前缀则无法匹配，JitPack 被排除在解析之外，表现为制品存在但依赖无法解析。应使用 `includeGroupByRegex("com\\.github\\.用户(\\..*)?")` 或写全 group。
