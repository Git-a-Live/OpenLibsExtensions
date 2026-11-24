# 开发与贡献

面向仓库贡献者与维护者。使用方文档见根目录 [README](../README.md)，发布流程见 [PUBLISHING](PUBLISHING.md)。

## 项目结构

```
OpenLibsExtensions/
├── coil/           # 渐变模糊：StackBlur + AGSL + Coil Transformation + ImageView 扩展
├── gson/           # 纯 Kotlin：安全取值 + 脏 JSON 修复
├── lottie/         # 共享 Composition 缓存
├── zxing/          # 二维码编码
├── kv/             # 键值存储：类型分发 + MMKV / DataStore 双后端
├── kv-gson/        # 可选编解码器：基于 Gson 反射，支持普通类
├── demo/           # View 体系示例 App，图片由代码生成，离线可运行
├── docs/           # 维护者文档
├── gradle/libs.versions.toml
└── .github/workflows/   # ci.yml / release.yml
```

## 构建

仓库的依赖解析顺序按环境区分为两种：

- 默认优先阿里云镜像，适配国内网络
- 检测到 `JITPACK=true` 环境变量时跳过镜像，直接使用官方源，避免海外构建机被镜像拖慢

```bash
./gradlew test                  # 82 个 JVM 单元测试
./gradlew :demo:assembleDebug   # 示例 APK：demo/build/outputs/apk/debug/demo-debug.apk
./gradlew publishToMavenLocal   # 验证发布配置：坐标、POM、sources jar
```

`demo` 包含四个页面，分别演示渐变模糊、JSON 修复、Lottie 列表与二维码。Android Studio 打开工程后选择 `demo` 运行。

CI 在每次提交执行 `test`、`:demo:assembleDebug`、`publishToMavenLocal`，任一失败即中断。`release.yml` 在 `v*` tag 上运行测试并创建 GitHub Release。

## 代码约定

### 显式 API 模式

四个库模块启用 `explicitApi()`，公开符号必须显式声明可见性与返回类型。省略 `public` 会导致编译失败，以此防止内部实现因默认可见性成为 ABI 的一部分。

公开 `inline` 函数需要访问的成员标注 `@PublishedApi internal`。

### JVM 目标

各模块的 `jvmTarget` 取自身依赖允许的最低值：

| 模块 | 目标 | 依据 |
|---|---|---|
| `:coil` | Java 11 | Coil 3 的字节码为 JVM 11，且 `ImageView.load` 为 inline 函数，低于 11 无法编译 |
| `:lottie` `:zxing` `:gson` `:kv` `:kv-gson` | Java 8 | 无更高版本的 API 需求 |
| `:demo` | Java 17 | 应用模块，不对外发布 |

公开 `inline` 函数会导致宿主受同一约束：Kotlin 禁止将高版本字节码内联进低版本目标。调整该配置需同步更新 README 的兼容性表。

### 测试

- 算法与分发逻辑一律通过 JVM 单元测试覆盖，不依赖设备
- 依赖 Android 框架的能力（`Bitmap`、`RenderEffect`、MMKV native）通过接口隔离后以假实现测试，例如 `KeyValueStore`
- DataStore 的测试使用内存实现，绕开其 Android 变体的原子文件操作在 JVM 单测中不可用的问题

## 设计取舍

| 需求 | 常见做法 | 本项目做法 | 代价 |
|---|---|---|---|
| 图片局部模糊 | 整图模糊后计算遮罩，或叠放两个 ImageView | 单控件内按比例分区 + 渐变过渡 | 需自行管理 `RenderEffect` 生命周期 |
| 脏 JSON | 捕获异常后丢弃字段 | 修复文本后重新解析 | 多一次解析；无法修复时仍明确返回 `null` |
| 列表 Lottie | 每个 item 配置 `lottie_rawRes` | 全局共享 Composition | 需在 `onBindViewHolder` / `onViewRecycled` 中显式调用 |
| 二维码 | `zxing-android-embedded` | 仅做编码 | 不含扫码 UI，扫码需另行接入 CameraX 等方案 |
| 键值存储序列化 | 固定依赖某一 JSON 库 | `KvCodec` 协议，默认 kotlinx.serialization，可替换 | 默认实现不支持普通类，需引入 `:kv-gson` |

## 已知限制

- GPU 模糊路径缺少真机逐像素比对，仅有编译期与逻辑验证
- `RuntimeShader` 在运行时编译，个别 GPU 驱动可能拒绝复杂着色器
- GitHub Actions 首次运行需确认托管 runner 是否预装 `compileSdk 37`，工作流中已包含补装步骤
