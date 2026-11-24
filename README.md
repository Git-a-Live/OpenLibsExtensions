# OpenLibsExtensions

[English](README_EN.md) | 中文

对 Coil、Gson、Moshi、Lottie、ZXing、MMKV / DataStore、WorkManager 的轻量封装，并提供协程任务索引与试听区间进度条，拆分为互不依赖的 Gradle 模块：渐变模糊、脏 JSON 容错、列表动画共享缓存、二维码编码、键值存储。

各模块对应实际项目中反复出现的四类问题：低版本缺乏 GPU 模糊路径、服务端偶发返回损坏 JSON、列表内数十个 Lottie 实例各自解析动画、二维码叠加中心图标后识别率下降。除 `:kv-gson` 依赖 `:kv` 外，模块间无依赖关系，均可单独引入。

## 目录

- [能力](#能力)
- [兼容性](#兼容性)
- [依赖契约](#依赖契约)
- [模块](#模块)
- [接入](#接入)
- [相关文档](#相关文档)
- [License](#license)

## 能力

- **依赖面小**：`:gson` 为纯 Kotlin/JVM 模块，可脱离 Android 使用；其余模块仅依赖对应三方库，不引入图片、Camera 等大包
- **双路径同口径**：模糊同时提供 GPU（AGSL）与 CPU（StackBlur）实现，半径按占高度比例换算，同一份配置在不同尺寸控件上观感一致
- **可降级**：API 不足时自动回退，调用方无需编写版本分支

## 兼容性

| 项目 | 要求 |
|---|---|
| minSdk | 21 |
| compileSdk | 37 |
| 宿主 `jvmTarget` | `:coil` ≥ **11**，其余模块 ≥ **1.8** |
| AGP | 9.5.1（内建 Kotlin，不应用 `kotlin-android` 插件） |
| Gradle | 9.6.0 |
| Kotlin | 2.4.21 |
| JDK | 17+ |
| 模糊 GPU 路径 | API 33+（`RuntimeShader`），低版本自动走 CPU |

`jvmTarget` 下限的来源：公开 API 含 `inline` 函数（`toListOrNull<T>()`、`toKey` 等），Kotlin 禁止将高版本字节码内联进低版本目标，宿主目标低于表中下限时编译失败。`:coil` 的 11 由 Coil 3 锁定，其字节码为 JVM 11 且 `ImageView.load` 为 inline 函数。

## 依赖契约

产物为薄 AAR/JAR，不 shade、不做 fat-jar。第三方库以 Gradle 依赖传递，最终版本由宿主的冲突解决决定。

| 模块 | 编译时依赖 | 声明方式 |
|---|---|---|
| `:coil` | `io.coil-kt.coil3:coil:3.6.3`、`androidx.annotation:1.11.0` | `requires`（下限） |
| `:lottie` | `com.airbnb.android:lottie:6.7.1`、`androidx.annotation:1.11.0` | `requires`（下限） |
| `:zxing` | `com.google.zxing:core:3.5.4` | `requires`（下限） |
| `:gson` | `com.google.code.gson:gson:2.15.0` | `requires`（下限） |
| `:moshi` | `com.squareup.moshi:moshi:1.15.2` | `requires`（下限） |
| `:kv` | `com.tencent:mmkv:2.4.3`、`androidx.datastore:datastore-preferences:1.3.0`、`org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0` | `requires`（下限） |
| `:kv-gson` | `:kv`、`com.google.code.gson:gson:2.15.0` | `requires`（下限） |
| `:coroutines` | `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0` | `requires`（下限） |
| `:workmanager` | `androidx.work:work-runtime:2.12.0`、`androidx.annotation:1.11.0` | `requires`（下限） |
| `:trial` | 无第三方依赖 | — |
| `:trial-compose` | `:trial`、Compose BOM 2026.09.00 | `requires`（下限） |

`requires` 表示下限而非版本锁：宿主使用更高版本时由 Gradle 取高者，本库不会降级；使用更低版本时会被抬至表中版本。

`kotlin-stdlib` 会被抬至 2.4.21，宿主 Kotlin 编译器需为 2.x。这由库的编译版本决定，无法通过依赖声明规避。

唯一的 androidx 传递依赖是 `annotation`（公开签名含 `@DrawableRes` / `@RawRes` / `@RequiresApi`），不引入 `androidx.core`。

宿主需停留在更低版本时，可排除后显式提供：

```kotlin
implementation("com.github.git-a-live.openlibsextensions:coil:v0.1.6") {
    exclude(group = "io.coil-kt.coil3", module = "coil")
}
implementation("io.coil-kt.coil3:coil:3.2.0")
```

低于表中版本时不保证二进制兼容：`Transformation`、`LottieDrawable` 等父类与方法签名可能不存在或语义已变。

## 模块

| 模块 | 依赖 | 能力 | 文档 |
|---|---|---|---|
| `:coil` | Coil 3.x | 顶部/底部渐变模糊、渐变色叠加、GPU(AGSL) 与 CPU(StackBlur) 双路径 | [文档](docs/modules/coil.md) |
| `:gson` | Gson | 安全取值扩展、三类脏 JSON 修复（非法转义 / 未转义引号 / 尾部截断） | [文档](docs/modules/gson.md) |
| `:moshi` | Moshi | 序列化入口与无类型映射的安全取值 | [文档](docs/modules/moshi.md) |
| `:lottie` | Lottie | 按 `资源 id + 日夜模式` 共享 Composition，按需播放与回收释放 | [文档](docs/modules/lottie.md) |
| `:zxing` | ZXing core | 二维码生成，支持中心图标与纠错等级配置 | [文档](docs/modules/zxing.md) |
| `:kv` | MMKV / DataStore / kotlinx.serialization | 泛型中缀键值读写，按运行时类型选择存储方式 | [文档](docs/modules/kv.md) |
| `:kv-gson` | `:kv` + Gson | 可选编解码器，支持未标注 `@Serializable` 的普通类 | [文档](docs/modules/kv.md) |
| `:coroutines` | kotlinx-coroutines | 按标识登记与取消协程任务，注册表自动清理 | [文档](docs/modules/coroutines.md) |
| `:workmanager` | WorkManager | 请求构造与约束拼装，worker 类型由调用点推导 | [文档](docs/modules/workmanager.md) |
| `:trial` | 无 | 试听区间进度条（View），区间内外配色与端点样式可配 | [文档](docs/modules/trial.md) |
| `:trial-compose` | `:trial` + Compose | 同一控件的 Compose 实现 | [文档](docs/modules/trial.md) |

各模块的英文文档为同目录下的 `*_EN.md`。

## 接入

### 1. 前置条件

- `minSdk` 21 及以上
- `compileSdk` 37 及以上（AAR 的 `aar-metadata` 中 `minCompileSdk=37`，低版本 AGP 直接报错）
- JDK 17 及以上

### 2. 添加仓库

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            // 实际 group 为 com.github.git-a-live.openlibsextensions（多模块在前缀后追加仓库名）。
            // includeGroup 为精确匹配，此处使用正则覆盖前缀与完整 group
            content { includeGroupByRegex("com\\.github\\.git-a-live(\\..*)?") }
        }
    }
}
```

限定 group 用于避免 JitPack 参与无关依赖的解析。省略该过滤条件同样可用，但同步阶段会向 JitPack 发起额外请求。

### 3. 添加依赖

模块按需引入：

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.git-a-live.openlibsextensions:coil:v0.1.6")    // 渐变模糊
    implementation("com.github.git-a-live.openlibsextensions:lottie:v0.1.6")  // 共享缓存
    implementation("com.github.git-a-live.openlibsextensions:zxing:v0.1.6")   // 二维码
    implementation("com.github.git-a-live.openlibsextensions:kv:v0.1.6")      // 键值存储
    implementation("com.github.git-a-live.openlibsextensions:kv-gson:v0.1.6") // 可选：支持普通类
    implementation("com.github.git-a-live.openlibsextensions:gson:v0.1.6")    // 脏 JSON 修复
    implementation("com.github.git-a-live.openlibsextensions:moshi:v0.1.6")   // Moshi 序列化与取值
    implementation("com.github.git-a-live.openlibsextensions:coroutines:v0.1.6")   // 协程任务索引
    implementation("com.github.git-a-live.openlibsextensions:workmanager:v0.1.6")  // WorkManager 请求构造
    implementation("com.github.git-a-live.openlibsextensions:trial:v0.1.6")        // 试听区间进度条
    implementation("com.github.git-a-live.openlibsextensions:trial-compose:v0.1.6") // 同上，Compose 版
}
```

坐标格式为 `com.github.git-a-live.openlibsextensions:模块名:版本`。

| 版本写法 | 含义 | 适用场景 |
|---|---|---|
| `v0.1.6` | Release tag | 生产环境 |
| `a1b2c3d` | 提交短哈希 | 验证单个修复 |
| `main-SNAPSHOT` | 分支最新提交 | 跟进开发分支 |

坐标统一小写。JitPack 的路径大小写不敏感（`com.github.philjay/mpandroidchart` 与 `com.github.PhilJay/MPAndroidChart` 返回相同内容），Maven 约定要求小写，本文档统一以小写书写。

### 4. 集成故障排查

| 现象 | 原因与处理 |
|---|---|
| 依赖解析 404 或长时间无响应 | 该 tag 首次被请求时 JitPack 开始构建，通常 1~3 分钟。构建日志见 `https://jitpack.io/com/github/git-a-live/openlibsextensions/v0.1.6/build.log` |
| 新 tag 解析到旧内容 | JitPack 存在缓存，使用 `--refresh-dependencies` 重新解析 |
| `Dependency requires compileSdk 37` | 宿主 `compileSdk` 低于 37 |
| 找不到 `coil3.transform.Transformation` | 宿主引入了其他版本的 Coil，被解析为低版本；用 `./gradlew :app:dependencies` 确认实际版本 |

## Compose 支持

`GradientBlurTransformation` 是标准 Coil `Transformation`，`AsyncImage` 可直接使用；`GradientBlurEffect`（AGSL）为 View 层实现。`:trial-compose` 提供试听区间进度条的 Compose 实现，其余模块与 UI 框架无关。

## 相关文档

- [docs/modules/](docs/modules/)：各模块的用法、API 与限制
- [docs/DEVELOPING.md](docs/DEVELOPING.md)：项目结构、构建与测试、代码约定、设计取舍
- [docs/PUBLISHING.md](docs/PUBLISHING.md)：坐标配置、tag 流程、JitPack 构建机制

## License

[MIT](LICENSE)
