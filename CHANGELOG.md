# Changelog

版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。Kotlin 库存在一项二进制兼容约束：为公开类型新增带默认值的构造器参数，对 Kotlin 调用方构成二进制破坏，此类改动升 minor。

## 0.1.4

### 新增

- `:coroutines` 协程任务索引。`TaskTag` 值类型标识、任务结束时自动移除注册表条目、
  同一标识为替换语义；周期任务区分 `FixedRate`（按启动时刻排期）与 `FixedDelay`。
- `:workmanager` 请求构造。`reified` 取得具体 worker 类型，退避与周期参数在构造期校验下限，
  `WorkScheduler` 提供初始化与按标签、唯一名称取消的入口。

## 0.1.3

### 新增

- `:moshi` 序列化模块，与 `:gson` 同形但按 Moshi 的模型建模：序列化入口基于 `JsonAdapter`，
  树模型为无类型 `Map<String, Any?>`，取值方法兼容 Moshi 将数字统一解析为 `Double` 的行为。
  不引入 kotlin-reflect，需要反射支持时由调用方注入 `KotlinJsonAdapterFactory`。

## 0.1.2

### 新增

- `:kv` 键值存储模块。泛型中缀读写（`42 toKey "age"` / `getKV<Int>("age")` / `String::class fromKey "age"`），
  写入按运行时类型自适应选择 MMKV 的原生编码接口，其余类型用 kotlinx.serialization 序列化为 JSON（目标类需标注 `@Serializable`）。
- 提供 MMKV（同步）与 DataStore（挂起）两个后端，共用同一套类型分发规则，
  语法一致、导入不同包即可切换。
- `:kv` 不依赖 Gson，序列化层只用 kotlinx.serialization，避免运行时反射序列化在 R8 下的额外 keep 成本。
- 新增 `KvCodec` 编解码协议：确认 `Kv.useCodec(...)` 可替换复合类型的序列化实现，两个后端共用。
- 新增 `:kv-gson` 模块，提供基于 Gson 反射的 `GsonKvCodec`，使未标注 `@Serializable`
  的普通类可读写；`:kv` 主模块不引入该依赖。
- 泛型集合修复：`List<T>` 等复合类型此前查不到元素序列化器、静默不存储；
  现在通过 `typeOf<T>()` 把完整类型信息传给编解码器，`getKV<List<T>>` 可正常往返。

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
