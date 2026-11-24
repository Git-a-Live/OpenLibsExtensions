# OpenLibsExtensions

对 Coil、Gson、Lottie、ZXing、MMKV / DataStore 的轻量封装，拆分为互不依赖的 Gradle 模块：渐变模糊、脏 JSON 容错、列表动画共享缓存、二维码编码、键值存储。

各模块对应实际项目中反复出现的四类问题：低版本缺乏 GPU 模糊路径、服务端偶发返回损坏 JSON、列表内数十个 Lottie 实例各自解析动画、二维码叠加中心图标后识别率下降。除 `:kv-gson` 依赖 `:kv` 外，模块间无依赖关系，均可单独引入。

## 目录

- [能力](#能力)
- [兼容性](#兼容性)
- [依赖契约](#依赖契约)
- [模块](#模块)
- [接入](#接入)
- [coil：渐变模糊](#coil渐变模糊)
- [gson：脏 JSON 容错](#gson脏-json-容错)
- [moshi：序列化与安全取值](#moshi序列化与安全取值)
- [lottie：列表共享缓存](#lottie列表共享缓存)
- [zxing：二维码](#zxing二维码)
- [kv：键值存储](#kv键值存储)
- [常见问题](#常见问题)
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

`requires` 表示下限而非版本锁：宿主使用更高版本时由 Gradle 取高者，本库不会降级；使用更低版本时会被抬至表中版本。

`kotlin-stdlib` 会被抬至 2.4.21，宿主 Kotlin 编译器需为 2.x。这由库的编译版本决定，无法通过依赖声明规避。

唯一的 androidx 传递依赖是 `annotation`（公开签名含 `@DrawableRes` / `@RawRes` / `@RequiresApi`），不引入 `androidx.core`。

宿主需停留在更低版本时，可排除后显式提供：

```kotlin
implementation("com.github.git-a-live.openlibsextensions:coil:v0.1.2") {
    exclude(group = "io.coil-kt.coil3", module = "coil")
}
implementation("io.coil-kt.coil3:coil:3.2.0")
```

低于表中版本时不保证二进制兼容：`Transformation`、`LottieDrawable` 等父类与方法签名可能不存在或语义已变。

## 模块

| 模块 | 依赖 | 能力 |
|---|---|---|
| `:coil` | Coil 3.x | 顶部/底部渐变模糊、渐变色叠加、GPU(AGSL) 与 CPU(StackBlur) 双路径 |
| `:gson` | Gson | 安全取值扩展、三类脏 JSON 修复（非法转义 / 未转义引号 / 尾部截断） |
| `:moshi` | Moshi | 序列化入口与无类型映射的安全取值 |
| `:lottie` | Lottie | 按 `资源 id + 日夜模式` 共享 Composition，按需播放与回收释放 |
| `:zxing` | ZXing core | 二维码生成，支持中心图标与纠错等级配置 |
| `:kv` | MMKV / DataStore / kotlinx.serialization | 泛型中缀键值读写，按运行时类型选择存储方式 |
| `:kv-gson` | `:kv` + Gson | 可选编解码器，支持未标注 `@Serializable` 的普通类 |

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
    implementation("com.github.git-a-live.openlibsextensions:coil:v0.1.2")    // 渐变模糊
    implementation("com.github.git-a-live.openlibsextensions:lottie:v0.1.2")  // 共享缓存
    implementation("com.github.git-a-live.openlibsextensions:zxing:v0.1.2")   // 二维码
    implementation("com.github.git-a-live.openlibsextensions:kv:v0.1.2")      // 键值存储
    implementation("com.github.git-a-live.openlibsextensions:kv-gson:v0.1.2") // 可选：支持普通类
    implementation("com.github.git-a-live.openlibsextensions:gson:v0.1.2")    // 脏 JSON 修复
    implementation("com.github.git-a-live.openlibsextensions:moshi:v0.1.2")   // Moshi 序列化与取值
}
```

坐标格式为 `com.github.git-a-live.openlibsextensions:模块名:版本`。

| 版本写法 | 含义 | 适用场景 |
|---|---|---|
| `v0.1.2` | Release tag | 生产环境 |
| `a1b2c3d` | 提交短哈希 | 验证单个修复 |
| `main-SNAPSHOT` | 分支最新提交 | 跟进开发分支 |

坐标统一小写。JitPack 的路径大小写不敏感（`com.github.philjay/mpandroidchart` 与 `com.github.PhilJay/MPAndroidChart` 返回相同内容），Maven 约定要求小写，本文档统一以小写书写。

### 4. 本地联调

不等待 JitPack 构建，或需修改源码验证时，使用本地仓库：

```bash
# OpenLibsExtensions 仓库根目录
./gradlew publishToMavenLocal
```

```kotlin
// 宿主工程 settings.gradle.kts
repositories {
    mavenLocal()   // 置于 google()/mavenCentral() 之前才会优先命中
}
```

### 5. 集成故障排查

| 现象 | 原因与处理 |
|---|---|
| 依赖解析 404 或长时间无响应 | 该 tag 首次被请求时 JitPack 开始构建，通常 1~3 分钟。构建日志见 `https://jitpack.io/com/github/git-a-live/openlibsextensions/v0.1.2/build.log` |
| 新 tag 解析到旧内容 | JitPack 存在缓存，使用 `--refresh-dependencies` 重新解析 |
| `Dependency requires compileSdk 37` | 宿主 `compileSdk` 低于 37 |
| 找不到 `coil3.transform.Transformation` | 宿主引入了其他版本的 Coil，被解析为低版本；用 `./gradlew :app:dependencies` 确认实际版本 |

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

列表场景在回收时释放 GPU 缓冲，重新可见且未重新加载图片时恢复：

```kotlin
override fun onViewRecycled(holder: Holder) {
    holder.image.releaseGradientBlur()
}

imageView.restoreGradientBlur()
```

### API

| 调用 | 说明 |
|---|---|
| `ImageView.loadWithGradientBlur(data, options, ...)` | 加载并施加渐变模糊，自动选择 GPU / CPU |
| `ImageView.releaseGradientBlur()` / `restoreGradientBlur()` | 释放 GPU 缓冲 / 重新挂载效果 |
| `BitmapBlur.blur(bitmap, radius, sampling)` | 位图模糊，返回新位图 |
| `StackBlur.blur(pixels, width, height, radius)` | 像素级算法，不依赖 `android.graphics`，可直接单测 |
| `GradientBlurTransformation(...)` | Coil `Transformation`，可用于 `AsyncImage` |

### 两条路径

| 路径 | 实现 | 要求 | 适用场景 |
|---|---|---|---|
| GPU | `GradientBlurEffect`（AGSL + `RenderEffect`） | API 33+ | 列表、播放页背景等实时渲染 |
| CPU | `GradientBlurTransformation`（Coil `Transformation`） | 无 | 低版本兜底、导出静态位图 |

`GradientBlurOptions.preferGpu = true`（默认）时，API 33+ 走 GPU，低版本回退 CPU，调用方无需分支。

### 实现说明

- CPU 侧为三角核可分离 Stack Blur，复杂度 `O(1)/像素`；按 `sampling` 降采样后模糊，再放大回原尺寸。1080×1080 的封面在 `sampling = 4` 时参与运算的像素由 117 万降至 7.3 万。
- GPU 侧以黄金角螺旋在圆盘内布置 16 个采样点，配径向高斯权重；相较多层网格采样，采样次数降低并借助纹理线性过滤放大等效采样数。
- 组合顺序为「模糊 → 叠色 → 统一裁切」。叠色发生在遮罩之前，模糊与颜色共用同一条渐变边界并一同淡出，清晰区边缘不出现颜色截断。
- `RenderEffect` 的中间缓冲按视图尺寸分配、依赖 finalizer 释放，列表快速滚动时会累积，因此提供显式 `release` / `restore`。实例按参数缓存在 view tag 上，参数未变时复用已编译的 shader。

### 限制

- GPU 路径的视觉效果未经真机逐像素比对，仅有编译期与逻辑验证；接入后应对比 `preferGpu = true / false` 两条路径。
- `RuntimeShader` 在运行时编译，个别 GPU 驱动可能拒绝复杂着色器。需要确定性时可按机型降级至 CPU 路径。

## gson：脏 JSON 容错

服务端返回的 JSON 有三类常见损伤，修复器逐类处理，并按两两组合、三三组合降级尝试：

| 损伤 | 示例 | 处理 |
|---|---|---|
| 非法转义 | `"path":"C:\Users\name"` | 将非法 `\` 转义为 `\\`；残缺 `\u` 降级为普通反斜杠 |
| 未转义引号 | `"desc":"他称"收到"后离开"` | 跳过空白检查后继字符，非 `, } ] :` 时判定为内容引号并补转义 |
| 尾部截断 | `{"a":1,"b":` | 按容器栈逆序补闭合符号；悬空成员回退至上一逗号重试 |

```kotlin
val result = JsonRepair.repair(raw) ?: return
if (!result.isUnmodified) {
    log("命中策略：${result.applied.joinToString { it.name }}")
}
parse(result.text)
```

`applied` 记录本次命中的修复类型，可用于统计线上脏数据分布。

### API

| 调用 | 说明 |
|---|---|
| `JsonRepair.repair(raw)` | 按策略链修复，返回 `Result?`；`null` 表示放弃 |
| `JsonRepair.escapeInvalidSequences` / `escapeUnescapedQuotes` / `closeTruncated` | 单项修复，无需修复时返回 `null` |
| `JsonObject.stringOr / intOr / longOr / doubleOr / floatOr / booleanOr` | 安全取值，类型不符时回退默认值 |
| `JsonObject.objectOrNull / arrayOrNull / elementOrNull` | 结构取值，区分「缺失」与「显式 null」 |
| `String?.toJsonObjectOrNull()` / `toListOrNull<T>()` / `toObjectOrNull<T>()` | 解析入口，失败返回 `null` |
| `Any?.toJsonOrNull()` | 序列化，关闭 HTML 转义 |

字段缺失、为 `null`、类型不符均不抛异常：

```kotlin
val name = json.stringOr("name")     // 缺失或 null 时返回 ""
val count = json.intOr("count", -1)  // 非数字字符串同样回退默认值
val tags = json.arrayOrNull("tags")  // 类型不符返回 null，不抛 ClassCastException
```

### 限制

- 修复目标是「能被标准解析器接受」，不做语义推断：`{"a":,"b":1}` 这类缺少值的结构不补丁，直接返回 `null`。
- 合法转义保持原样，因此 Windows 路径中的 `\n`、`\t` 仍会被解析为换行与制表符。此类内容应在协议层面转义。

## moshi：序列化与安全取值

Moshi 不对任意对象做反射建模，每个类型都需要 `JsonAdapter`（代码生成或手动注册）。本模块在此前提下提供序列化入口与无类型映射的安全取值。

```kotlin
@JsonClass(generateAdapter = true)
data class User(val name: String, val age: Int)

User("n", 1).toJsonOrNull()
"""{"name":"n","age":1}""".toObjectOrNull<User>()
```

### API

| 调用 | 说明 |
|---|---|
| `T.toJsonOrNull()` / `T.toJsonOrEmpty()` | 按调用点类型序列化，失败返回 `null` / 空串 |
| `T.toJsonOrNull(moshi, type)` | 显式指定 `Moshi` 实例与类型 |
| `String?.toObjectOrNull<T>()` | 反序列化，失败返回 `null` |
| `String?.toObjectOrNull(clazz)` | 非泛型类型的反序列化 |
| `String?.toListOrNull<T>()` | 反序列化为 `List<T>` |
| `String?.toMapOrNull()` | 反序列化为 `Map<String, Any?>` |
| `Map<String, Any?>.stringOr / intOr / longOr / doubleOr / floatOr / booleanOr` | 安全取值，类型不符时回退默认值 |
| `Map<String, Any?>.mapOrNull / listOrNull` | 结构取值 |
| `MoshiKit.useMoshi(moshi)` | 替换全局实例 |

### 无类型映射的取值

Moshi 默认将所有数字解析为 `Double`，因此 `intOr` 等取值同时兼容 `Double`、`Long`、`Int` 与数字字符串，不依赖单一类型判断：

```kotlin
val map = json.toMapOrNull() ?: return
map.intOr("count")       // JSON 中的 3 会解析为 3.0，此处取出 3
map.stringOr("name")     // 缺失或类型不符时返回 ""
map.booleanOr("flag")    // 兼容 "true" / "TRUE" 字符串
```

### 与 `:gson` 的差异

| 维度 | `:gson` | `:moshi` |
|---|---|---|
| 任意对象序列化 | 反射支持，无需注册 | 需要 `JsonAdapter`（codegen 或注册） |
| 树模型 | `JsonObject` 强类型 | `Map<String, Any?>` 无类型 |
| 数字解析 | 按整数与浮点区分 | 默认统一为 `Double` |
| 脏 JSON 修复 | 提供 | 不提供，可配合 `:gson` 的 `JsonRepair` |

未使用代码生成时，可通过 `MoshiKit.useMoshi(...)` 注入添加了 `KotlinJsonAdapterFactory` 的实例。该工厂依赖 kotlin-reflect，本模块不引入。

## lottie：列表共享缓存

```kotlin
// 布局中不配置 app:lottie_rawRes 与 app:lottie_autoPlay
override fun onBindViewHolder(holder: Holder, position: Int) {
    LottieCache.play(holder.lottie, R.raw.spinner)
}

override fun onViewRecycled(holder: Holder) {
    LottieCache.release(holder.lottie)
}
```

- 以 `资源 id + 日夜模式` 为键全局只解析一次。缓存键包含日夜模式，是因为 `raw/` 与 `raw-night/` 下同名资源共用同一个 `resId`，仅按 id 缓存会在切换深浅色后命中另一模式的 Composition
- 解析异步执行，期间登记的视图在解析完成后统一挂载，不阻塞主线程
- 按 Composition 自身帧率渲染（`setUseCompositionFrameRate`），30fps 素材在 60Hz 屏幕上不做两倍更新
- `onTrimMemory(TRIM_MEMORY_COMPLETE)` 时可调用 `LottieCache.clear()`

## zxing：二维码

```kotlin
val bitmap = QrCodes.encode(
    content = "https://example.com",
    options = QrCodeOptions(sizePx = 512, quietZoneModules = 1),
    centerIcon = logo,
    iconRatio = 0.18f,
)
```

- 默认纠错等级 H（约 30% 容错），为叠加中心图标的前提；图标占比被钳制在 `[0.1, 0.25]`
- 图标外围自动留出一圈背景色，避免与相邻码元贴边导致识别率下降
- 通过 `setPixels` 一次性写入像素数组，较逐点绘制减少调用次数
- 仅依赖 `com.google.zxing:core`，不引入 `zxing-android-embedded` 等含 Camera 的依赖

## kv：键值存储

两套后端共享同一套类型分发规则：MMKV（同步）与 DataStore（挂起）。

```kotlin
// 初始化，二选一
Kv.init(context)              // MMKV 后端，同步
DataStoreKv.init(dataStore)   // DataStore 后端，挂起

42 toKey "age"                 // Int 走原生整型编码
"北京" toKey "city"            // String 原样存储
User("n", 1) toKey "user"      // 其余类型交由编解码器

val age: Int? = getKV("age")
val user: User? = getKV("user")
val city: String? = String::class fromKey "city"
```

### 后端差异

| 后端 | 包 | 写入 | 读取 |
|---|---|---|---|
| MMKV | `io.libwrap.kv` | 同步 | 同步 |
| DataStore | `io.libwrap.kv.datastore` | `suspend` | `suspend` |

语法一致，导入对应包的 `toKey` / `getKV` 即切换后端。DataStore 版本必须为挂起函数：其读取基于 `Flow<Preferences>`、写入为挂起事务，不具备同步读取能力；以内存快照模拟同步读会导致写入后立即读取返回旧值。MMKV 基于 mmap，无此限制。

### 类型分发

基础类型（String / Int / Long / Float / Double / Boolean）按运行时类型分发，不经编解码器。因此值声明为 `Any` 时仍按真实类型落库：

```kotlin
val v: Any = 42
v toKey "k"          // 按 Int 存储
getKV<Int>("k")      // 42
```

复合类型交由 `KvCodec`，`typeOf<T>()` 的完整类型信息（含泛型参数）会传递至编解码器。

### 序列化

默认实现为 kotlinx.serialization，仅支持标注 `@Serializable` 的类型：

```kotlin
@Serializable
data class User(val name: String, val age: Int)

User("n", 1) toKey "user"            // JSON 字符串
listOf(User("a", 1)) toKey "users"   // 泛型集合

val user: User? = getKV("user")
val users: List<User>? = getKV("users")
```

未标注 `@Serializable` 的类型不会被写入，也不抛出异常。需要支持普通类时，引入可选模块并替换编解码器：

```kotlin
implementation("com.github.git-a-live.openlibsextensions:kv-gson:v0.1.2")

Kv.useCodec(GsonKvCodec())   // 初始化阶段设置一次

Plain(1, "a") toKey "plain"
val plain: Plain? = getKV("plain")
```

Gson 走反射序列化，宿主需在 R8 配置中保留数据类的无参构造与字段名。Gson 自带的 `gson.pro` 仅覆盖库内部路径，不覆盖业务类。

编解码器为进程级配置，MMKV 与 DataStore 两个后端共用同一份。

### 约定

- 同一 key 必须使用同一类型读写。存储层不记录值的类型，类型不符时的读取结果未定义。
- `putKV(key, null)` 表示删除该键，而非写入 null。

## 常见问题

**与自研 Coil `BlurTransformation` 的差异**

多数实现只做整图模糊。本模块面向「仅模糊顶部区间、并与清晰区之间保留渐变过渡」这类带几何描述的需求，同时保证 GPU 与 CPU 两条路径的半径口径一致。

**GPU 与 CPU 的像素结果是否一致**

不一致，也不以逐像素一致为目标。GPU 为 16 点螺旋采样，CPU 为三角核可分离卷积，核函数不同；两者一致的是几何描述（分区比例、过渡带位置、叠色方向），因此观感一致。需要可复现的逐像素结果时固定使用 CPU 路径。

**修复器对合法 JSON 的影响**

无影响。第一步为严格校验，合法文本原样返回（`applied` 为空）；每个修复候选同样需通过校验，未通过则丢弃。

**`releaseGradientBlur()` 之后图片是否消失**

不消失。该方法只卸载 GPU 中间缓冲，`RenderEffect` 移除后视图回到无效果状态，图片仍在。重新可见时调用 `restoreGradientBlur()` 恢复，无需重新解码。

**minSdk 21 与模糊所需的 API 33 之间的落差**

`preferGpu = true` 时低版本自动使用 CPU 的 `GradientBlurTransformation`，接口不变。需要统一路径时设 `preferGpu = false`。

**Compose 支持情况**

`GradientBlurTransformation` 为标准的 Coil `Transformation`，`AsyncImage` 可直接使用；`GradientBlurEffect`（AGSL）目前为 View 层实现。

## 相关文档

- [docs/DEVELOPING.md](docs/DEVELOPING.md)：项目结构、构建与测试、代码约定、设计取舍
- [docs/PUBLISHING.md](docs/PUBLISHING.md)：坐标配置、tag 流程、JitPack 构建机制

## License

[MIT](LICENSE)
