# OpenLibsExtensions

对四个常用三方库的轻量封装：**Coil 渐变模糊**、**Gson 脏数据容错**、**Lottie 列表共享缓存**、**ZXing 二维码**。

这些封装来自真实项目里踩过的坑，共同点是"库本身没问题，但默认用法在某些场景下会出问题"：模糊在低版本没有 GPU 路径、服务端偶发返回坏 JSON、列表里几十个 Lottie 实例各自解析动画、二维码叠了中心图标就扫不出来。本项目把解法抽成可复用模块，每个模块只做一件事，不引入额外的架构约束。

## 目录

- [特性](#特性)
- [兼容性](#兼容性)
- [依赖契约](#依赖契约)
- [模块](#模块)
- [接入](#接入)
- [coil：渐变模糊](#coil渐变模糊)
- [gson：脏 JSON 容错](#gson脏-json-容错)
- [lottie：列表共享缓存](#lottie列表共享缓存)
- [zxing：二维码](#zxing二维码)
- [与同类方案的取舍](#与同类方案的取舍)
- [项目结构](#项目结构)
- [构建与验证](#构建与验证)
- [常见问题](#常见问题)
- [维护者文档](#维护者文档)
- [License](#license)

## 特性

- **最小依赖**：`gson` 模块是纯 Kotlin/JVM，可脱离 Android 使用；其余模块只依赖对应三方库本身，不额外引入图片/Camera 之类的大包
- **双路径且口径一致**：模糊同时提供 GPU（AGSL）与 CPU（StackBlur）实现，半径都按"占高度比例"换算，同一份配置在不同尺寸控件上观感一致
- **可验证**：60 个 JVM 单元测试覆盖算法与容错逻辑，其中 StackBlur 与暴力三角核参考实现逐像素比对，JSON 修复逐策略断言
- **可降级**：API 不足时自动回退，调用方不需要写版本分支

## 兼容性

| 项目 | 要求 |
|---|---|
| minSdk | 21 |
| compileSdk | 37 |
| AGP | 9.5.1（内建 Kotlin，不再应用 `kotlin-android` 插件） |
| Gradle | 9.6.0 |
| Kotlin | 2.4.21 |
| JDK | 17+（CI 使用 21） |
| 模糊 GPU 路径 | API 33+（`RuntimeShader`），低版本自动走 CPU |

## 依赖契约

产物是薄 AAR/JAR，**不 shade、不做 fat-jar**，第三方库以 Gradle 依赖的形式传递，最终版本由宿主的冲突解决决定。

| 模块 | 编译时依赖 | 声明方式 |
|---|---|---|
| `:coil` | `io.coil-kt.coil3:coil:3.6.3`、`androidx.annotation:1.11.0` | `requires`（下限） |
| `:lottie` | `com.airbnb.android:lottie:6.7.1`、`androidx.annotation:1.11.0` | `requires`（下限） |
| `:zxing` | `com.google.zxing:core:3.5.4` | `requires`（下限） |
| `:gson` | `com.google.code.gson:gson:2.15.0` | `requires`（下限） |

三点需要明确：

1. **不是版本锁**。`requires` 只声明下限：宿主用更高版本时 Gradle 取高者，本库不会强行降级；宿主用更低版本时会被抬到上表版本。
2. **`kotlin-stdlib` 会被抬到 2.4.21**，因此宿主的 Kotlin 编译器需要 ≥ 2.x。这是库用 Kotlin 2.4 编译的必然结果，无法通过依赖声明规避。
3. **不引入 `androidx.core`**。三个 Android 模块曾把它列为 `implementation`，但它在本库中并未被使用——只在运行时类路径上白白增加一个依赖，现已移除；当前唯一的 androidx 传递依赖是 `annotation`（公开签名里用到了 `@DrawableRes` / `@RawRes` / `@RequiresApi`）。

如果宿主必须停留在更低版本，可以自行排除并显式提供：

```kotlin
implementation("com.github.git-a-live.openlibsextensions:coil:v0.1.0") {
    exclude(group = "io.coil-kt.coil3", module = "coil")
}
implementation("io.coil-kt.coil3:coil:3.2.0")
```

> 注意：降到低于上表版本时本库不保证二进制兼容——`Transformation`、`LottieDrawable` 等父类与方法签名可能不存在，或语义已变。兼容性只对上表版本及以上成立。

## 模块

| 模块 | 依赖 | 能力 |
|---|---|---|
| `:coil` | Coil 3.x | 顶部/底部渐变模糊、渐变色叠加、GPU(AGSL) 与 CPU(StackBlur) 双路径 |
| `:gson` | Gson | 安全取值扩展、三类脏 JSON 修复（非法转义 / 未转义引号 / 尾部截断） |
| `:lottie` | Lottie | 按 `资源 id + 日夜模式` 共享 Composition，按需播放与回收释放 |
| `:zxing` | ZXing core | 二维码生成，支持中心图标与纠错等级配置 |

## 接入

### 1. 前置条件

- `minSdk` 21 及以上
- `compileSdk` 37 及以上（AAR 的 `aar-metadata` 里写明了 `minCompileSdk=37`，低版本 AGP 会直接报错）
- JDK 17 及以上

### 2. 添加仓库

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            // 限定 group，避免 JitPack 参与其他依赖的解析、拖慢同步
            content { includeGroup("com.github.git-a-live") }
        }
    }
}
```

### 3. 添加依赖

按需选模块，不必全加：

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.git-a-live.openlibsextensions:coil:v0.1.0")    // 渐变模糊，依赖 Coil
    implementation("com.github.git-a-live.openlibsextensions:lottie:v0.1.0")  // 共享缓存，依赖 Lottie
    implementation("com.github.git-a-live.openlibsextensions:zxing:v0.1.0")   // 二维码，依赖 zxing-core
    implementation("com.github.git-a-live.openlibsextensions:gson:v0.1.0")    // 脏 JSON 修复，依赖 Gson
}
```

坐标规则是 `com.github.git-a-live.openlibsextensions:模块名:版本`。版本可以填：

| 写法 | 含义 | 适用 |
|---|---|---|
| `v0.1.0` | Release tag | 生产使用 |
| `a1b2c3d` | 提交短哈希 | 临时验证某个修复 |
| `main-SNAPSHOT` | 分支最新提交 | 跟进开发分支，可能随时变化 |

坐标全部小写。JitPack 的路径大小写不敏感（`com.github.philjay/mpandroidchart` 与 `com.github.PhilJay/MPAndroidChart` 返回相同内容，实测），但 Maven 约定要求坐标小写，这里统一小写书写。

### 4. 本地联调（可选）

不想等 JitPack 构建、或者要改源码验证时，走本地仓库：

```bash
# 在 OpenLibsExtensions 仓库根目录
./gradlew publishToMavenLocal
```

```kotlin
// 宿主工程 settings.gradle.kts
repositories {
    mavenLocal()   // 放在 google()/mavenCentral() 之前才会优先命中
}
```

### 集成时的常见问题

| 现象 | 原因与处理 |
|---|---|
| 依赖解析 404 或一直转圈 | 该 tag 首次被请求时 JitPack 才开始构建，通常 1~3 分钟。日志见 `https://jitpack.io/com/github/git-a-live/openlibsextensions/v0.1.0/build.log` |
| 明明发了新 tag 却拿到旧内容 | JitPack 会缓存，加 `--refresh-dependencies` 重新解析 |
| `Dependency requires compileSdk 37` | 宿主 `compileSdk` 低于 37，升上去即可 |
| 类找不到 `coil3.transform.Transformation` | 宿主已引入其他版本的 Coil，被 Gradle 解析成了低版本；用 `./gradlew :app:dependencies` 确认实际版本 |

## coil：渐变模糊

### 用法

```kotlin
imageView.loadWithGradientBlur(
    data = coverUrl,
    options = GradientBlurOptions(
        blurRatio = 0.035f,                              // 模糊半径占图片高度的比例
        top = BlurBand(coverage = 0.45f, fade = 0.18f),  // 顶部 45% 模糊，18% 过渡带
        bottom = BlurBand(
            coverage = 0.55f,
            fade = 0.25f,
            tintStart = 0x00FFFFFF,
            tintEnd = 0xCC1B2A4A.toInt(),                // 渐变色叠加
        ),
        cornerRadiusPx = 28f,
    ),
)
```

列表场景在回收时释放 GPU 缓冲，重新可见时恢复：

```kotlin
override fun onViewRecycled(holder: Holder) {
    holder.image.releaseGradientBlur()
}

// 视图重新可见、但没有重新加载图片时
imageView.restoreGradientBlur()
```

### API 速查

| 调用 | 说明 |
|---|---|
| `ImageView.loadWithGradientBlur(data, options, ...)` | 加载并施加渐变模糊，自动选择 GPU / CPU |
| `ImageView.releaseGradientBlur()` / `restoreGradientBlur()` | 释放 GPU 缓冲 / 重新挂载效果 |
| `BitmapBlur.blur(bitmap, radius, sampling)` | 纯位图模糊，返回新位图 |
| `StackBlur.blur(pixels, width, height, radius)` | 纯像素算法，不依赖 `android.graphics`，可直接单测 |
| `GradientBlurTransformation(...)` | Coil `Transformation`，与 `AsyncImage` 等 Compose 场景通用 |

### 两条路径

| 路径 | 实现 | 要求 | 适用 |
|---|---|---|---|
| GPU | `GradientBlurEffect`（AGSL + `RenderEffect`） | API 33+ | 列表、播放页背景等实时渲染场景 |
| CPU | `GradientBlurTransformation`（Coil `Transformation`） | 无 | 低版本兜底、导出静态位图（分享卡片、通知大图） |

`GradientBlurOptions.preferGpu = true`（默认）时，API 33+ 走 GPU、低版本自动回退 CPU，调用方无需分支。

### 实现要点

- **CPU 侧**是三角核的可分离 Stack Blur，复杂度 `O(1)/像素`；先按 `sampling` 降采样再模糊，最后放大回原尺寸。1080×1080 的封面在 `sampling = 4` 时参与运算的像素从 117 万降到 7.3 万。
- **GPU 侧**用黄金角螺旋在圆盘内布 16 个采样点，配径向高斯权重；相比逐像素遍历 9×9 网格（81 次采样），采样次数降到 16 次，并借纹理线性过滤把等效采样数放大。
- **组合顺序**是"先模糊 → 再叠色 → 最后统一裁切"。叠色发生在遮罩之前，模糊与颜色共用同一条渐变边界，会一起淡出，不会出现颜色在清晰区边缘突然截断的接缝。
- **GPU 资源生命周期**：`RenderEffect` 的中间缓冲按视图尺寸分配、依赖 finalizer 释放，列表快速滚动会累积，因此提供显式的 `release` / `restore`；实例按参数缓存在 view tag 上，参数未变时复用已编译的 shader。

### 限制

- GPU 路径的视觉效果未经真机逐像素比对，只有编译期与逻辑验证；首次上真机建议对比 `preferGpu = true / false` 两条路径。
- `RuntimeShader` 的编译发生在运行时，个别 GPU 驱动可能拒绝复杂着色器。需要绝对稳妥时可按机型降级到 CPU 路径。

## gson：脏 JSON 容错

服务端返回的 JSON 有三类常见损伤，修复器逐类处理，并两两组合、三三组合降级尝试：

| 损伤 | 例子 | 处理 |
|---|---|---|
| 非法转义 | `"path":"C:\Users\name"` | 把非法的 `\` 转义为 `\\`；残缺的 `\u` 同样降级为普通反斜杠 |
| 未转义引号 | `"desc":"他说"你好"就走了"` | 跳过空白看下一个字符，非 `, } ] :` 则判定为内容引号并补转义 |
| 尾部截断 | `{"a":1,"b":` | 按容器栈逆序补闭合符号；悬空成员回退到上一个逗号重试 |

```kotlin
val result = JsonRepair.repair(raw) ?: return
if (!result.isUnmodified) {
    log("命中策略：${result.applied.joinToString { it.name }}")
}
parse(result.text)
```

`applied` 会告诉你命中的是哪几类修复，便于统计线上脏数据的分布。

### API 速查

| 调用 | 说明 |
|---|---|
| `JsonRepair.repair(raw)` | 按策略链修复，返回 `Result?`；`null` 表示放弃 |
| `JsonRepair.escapeInvalidSequences` / `escapeUnescapedQuotes` / `closeTruncated` | 单项修复，无需修复时返回 `null` |
| `JsonObject.stringOr / intOr / longOr / doubleOr / floatOr / booleanOr` | 安全取值，类型不符回退默认值 |
| `JsonObject.objectOrNull / arrayOrNull / elementOrNull` | 结构取值，区分"缺失"与"显式 null" |
| `String?.toJsonObjectOrNull()` / `toListOrNull<T>()` / `toObjectOrNull<T>()` | 解析入口，失败返回 `null` |
| `Any?.toJsonOrNull()` | 序列化，关闭 HTML 转义 |

字段缺失、为 `null`、类型不符都不会抛异常：

```kotlin
val name = json.stringOr("name")     // 缺失或 null 时返回 ""
val count = json.intOr("count", -1)  // 非数字字符串同样回退默认值
val tags = json.arrayOrNull("tags")  // 类型不符返回 null，不抛 ClassCastException
```

### 限制

- 修复以"能被标准解析器接受"为目标，不做语义猜测：`{"a":,"b":1}` 这类缺少值的结构不会被打补丁，直接返回 `null`。
- 合法的转义序列保持原样，因此 Windows 路径里的 `\n`、`\t` 仍会被解析成换行与制表符——这类内容需要在协议层面转义，不是修复器该承担的职责。

## lottie：列表共享缓存

```kotlin
// 布局里不要写 app:lottie_rawRes / app:lottie_autoPlay
override fun onBindViewHolder(holder: Holder, position: Int) {
    LottieCache.play(holder.lottie, R.raw.spinner)
}

override fun onViewRecycled(holder: Holder) {
    LottieCache.release(holder.lottie)
}
```

- 以 `资源 id + 日夜模式` 为键全局只解析一次。缓存键包含日夜模式，是因为 `raw/` 与 `raw-night/` 下同名资源共用同一个 `resId`，只按 id 缓存会在切换深浅色后命中另一模式的 Composition
- 解析是异步的，期间登记的视图会在解析完成后统一挂载，不阻塞主线程
- 按 Composition 自身帧率渲染（`setUseCompositionFrameRate`），30fps 素材在 60Hz 屏上不会做无意义的两倍更新
- `onTrimMemory(TRIM_MEMORY_COMPLETE)` 时可调 `LottieCache.clear()`

## zxing：二维码

```kotlin
val bitmap = QrCodes.encode(
    content = "https://example.com",
    options = QrCodeOptions(sizePx = 512, quietZoneModules = 1),
    centerIcon = logo,
    iconRatio = 0.18f,
)
```

- 默认纠错等级 H（约 30% 容错），这是叠加中心图标的前提；图标占比会被钳制在 `[0.1, 0.25]`
- 图标外围自动留一圈背景色，避免与相邻码元贴边导致识别率骤降
- 用 `setPixels` 一次性写入像素数组，比逐点绘制快一个量级
- 只依赖 `com.google.zxing:core`，不引入 `zxing-android-embedded` 这类带 Camera 的重依赖

## 与同类方案的取舍

| 需求 | 常见做法 | 本项目做法 | 取舍 |
|---|---|---|---|
| 图片局部模糊 | 整图模糊后自己算遮罩、或用两个上下叠放的 ImageView | 单控件内按比例分区 + 渐变过渡 | 少一层布局，但需要自己管理 `RenderEffect` 生命周期 |
| 脏 JSON | 捕获异常后丢字段 | 修复文本后重解析 | 能救回部分数据，代价是多一次解析；修不好仍然明确返回 `null` |
| 列表 Lottie | 每个 item 配 `lottie_rawRes` | 全局共享 Composition | 需要改 `onBindViewHolder` / `onViewRecycled`，换来的内存占用与首帧耗时下降 |
| 二维码 | `zxing-android-embedded` | 只做编码 | 不含扫码 UI，体积小；扫码需自行接 CameraX 或其它方案 |

## 项目结构

```
OpenLibsExtensions/
├── coil/           # 渐变模糊：StackBlur + AGSL + Coil Transformation + ImageView 扩展
├── gson/           # 纯 Kotlin：安全取值 + 脏 JSON 修复
├── lottie/         # 共享 Composition 缓存
├── zxing/          # 二维码编码
├── demo/           # View 体系示例 App（图片由代码生成，离线可跑）
├── docs/           # 维护者文档（发布与版本策略）
├── gradle/libs.versions.toml
└── .github/workflows/   # ci.yml / release.yml
```

## 构建与验证

仓库优先使用阿里云镜像以适配国内网络；检测到 `JITPACK=true` 环境变量时自动切回官方源，避免海外构建机被镜像拖慢。

```bash
./gradlew test                  # 60 个 JVM 单元测试
./gradlew :demo:assembleDebug   # 示例 APK：demo/build/outputs/apk/debug/demo-debug.apk
```

`demo` 四个页面分别演示模糊、JSON 修复、Lottie 列表、二维码。Android Studio 直接打开工程、选择 `demo` 运行即可。

CI 每次提交执行上述三条命令，任一失败即中断；`.github/workflows/release.yml` 在 `v*` tag 上跑测试并创建 GitHub Release。

## 常见问题

**Q：和自己写一个 Coil `BlurTransformation` 有什么区别？**
A：多数现成实现只做"整图模糊"。这里解决的是"只模糊顶部 40%、并且与清晰区之间要有渐变过渡"这类带几何描述的需求，同时保证 GPU 与 CPU 两条路径的半径口径一致。

**Q：GPU 与 CPU 的像素结果完全一致吗？**
A：不一致，也不追求一致。GPU 是 16 点螺旋采样，CPU 是三角核可分离卷积，两者的核函数不同；一致的是**几何描述**（分区比例、过渡带位置、叠色方向），所以观感一致。需要逐像素可复现时应固定走 CPU 路径。

**Q：修复器会不会把本来合法的 JSON 改坏？**
A：不会。第一步就是严格校验，合法文本原样返回（`applied` 为空）；每个修复候选同样要过校验，不通过就丢弃。

**Q：`releaseGradientBlur()` 之后图片会消失吗？**
A：不会。它只卸载 GPU 中间缓冲，`RenderEffect` 被移除后视图回到"无效果"状态，图片本身仍在。重新可见时调 `restoreGradientBlur()` 即可恢复，不需要重新解码。

**Q：minSdk 21 但模糊要 33，低版本怎么办？**
A：`preferGpu = true` 时低版本自动走 CPU 的 `GradientBlurTransformation`，接口不变。想强制统一路径就设 `preferGpu = false`。

**Q：Compose 能用吗？**
A：`GradientBlurTransformation` 是标准 Coil `Transformation`，`AsyncImage` 可以直接用；`GradientBlurEffect`（AGSL）目前是 View 层实现，Compose 侧需要等 `Modifier` 版本。

## 维护者文档

发布坐标配置、tag 流程、JitPack 构建机制、Maven Central 的前置条件，见 [docs/PUBLISHING.md](docs/PUBLISHING.md)。普通使用者不需要读这一节。

## License

[MIT](LICENSE)
