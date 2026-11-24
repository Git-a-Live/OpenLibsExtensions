# moshi：序列化与安全取值

[English](moshi_EN.md) | 中文

模块 `:moshi`，依赖 Moshi。

依赖坐标与仓库配置见[根目录 README](../../README.md#接入)。

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

---

[返回根目录 README](../../README.md)
