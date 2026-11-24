# moshi: serialization and safe access

English | [中文](moshi.md)

Module `:moshi`, depends on Moshi.

Coordinates and repository setup: [root README](../../README_EN.md#getting-started).

Moshi does not model arbitrary objects reflectively: every type needs a `JsonAdapter`, either generated or registered manually. This module provides serialization entry points and safe access over the untyped map model within that constraint.

```kotlin
@JsonClass(generateAdapter = true)
data class User(val name: String, val age: Int)

User("n", 1).toJsonOrNull()
"""{"name":"n","age":1}""".toObjectOrNull<User>()
```

### API

| Call | Description |
|---|---|
| `T.toJsonOrNull()` / `T.toJsonOrEmpty()` | Serializes using the call-site type; returns `null` / an empty string on failure |
| `T.toJsonOrNull(moshi, type)` | Explicit `Moshi` instance and type |
| `String?.toObjectOrNull<T>()` | Deserialization, returns `null` on failure |
| `String?.toObjectOrNull(clazz)` | Deserialization for non-generic types |
| `String?.toListOrNull<T>()` | Deserializes to `List<T>` |
| `String?.toMapOrNull()` | Deserializes to `Map<String, Any?>` |
| `Map<String, Any?>.stringOr / intOr / longOr / doubleOr / floatOr / booleanOr` | Safe access, falling back to a default on type mismatch |
| `Map<String, Any?>.mapOrNull / listOrNull` | Structural access |
| `MoshiKit.useMoshi(moshi)` | Replaces the global instance |

### Access over the untyped map

Moshi parses every number as `Double` by default, so `intOr` and friends accept `Double`, `Long`, `Int` and numeric strings rather than testing a single type:

```kotlin
val map = json.toMapOrNull() ?: return
map.intOr("count")       // 3 in JSON becomes 3.0, returned here as 3
map.stringOr("name")     // "" when missing or of the wrong type
map.booleanOr("flag")    // accepts the strings "true" / "TRUE"
```

### Differences from `:gson`

| Aspect | `:gson` | `:moshi` |
|---|---|---|
| Arbitrary objects | Reflective, no registration | Requires a `JsonAdapter` (codegen or registration) |
| Tree model | Typed `JsonObject` | Untyped `Map<String, Any?>` |
| Number parsing | Distinguishes integers from floating point | Uniformly `Double` by default |
| Malformed JSON repair | Provided | Not provided; combine with `JsonRepair` from `:gson` |

Without code generation, inject a `Moshi` instance carrying `KotlinJsonAdapterFactory` through `MoshiKit.useMoshi(...)`. That factory depends on kotlin-reflect, which this module does not pull in.

---

[Back to the root README](../../README_EN.md)
