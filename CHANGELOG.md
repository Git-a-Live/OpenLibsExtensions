# Changelog

版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。Kotlin 库需特别注意：给公开类型新增带默认值的构造器参数，对 Kotlin 调用方属于二进制破坏，这类改动会升 minor。

## 0.1.1

### 修复

- 降低 JVM 目标，修复宿主无法内联公开 API 的问题。`:gson` / `:lottie` / `:zxing` 改为 Java 8，`:coil` 改为 Java 11。此前统一使用 Java 17，导致 `jvmTarget` 低于 17 的宿主在调用 `toListOrNull<T>()`、`toObjectOrNull<T>()` 时报
  `Cannot inline bytecode built with JVM target 17 into bytecode that is being built with JVM target 11`。
  `:coil` 的下限 11 由 Coil 3 锁定，无法再降低。

### 文档

- 修正 JitPack 仓库的 group 过滤写法。`includeGroup` 是精确匹配，多模块项目的实际 group 是 `com.github.用户.仓库名`，必须用 `includeGroupByRegex` 或写全，否则 JitPack 会被排除在解析之外。

## 0.1.0

首个公开版本。

- `:coil` Coil 3 渐变局部模糊，AGSL（API 33+）与 StackBlur 双路径
- `:gson` 安全取值扩展与三类脏 JSON 修复
- `:lottie` 共享 `LottieComposition` 缓存
- `:zxing` 带中心图标的二维码编码
