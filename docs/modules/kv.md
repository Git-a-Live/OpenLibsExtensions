# kv：键值存储

[English](kv_EN.md) | 中文

模块 `:kv` 与 `:kv-gson`，依赖 MMKV / DataStore / kotlinx.serialization。

依赖坐标与仓库配置见[根目录 README](../../README.md#接入)。

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
implementation("com.github.git-a-live.openlibsextensions:kv-gson:v0.1.6")

Kv.useCodec(GsonKvCodec())   // 初始化阶段设置一次

Plain(1, "a") toKey "plain"
val plain: Plain? = getKV("plain")
```

Gson 走反射序列化，宿主需在 R8 配置中保留数据类的无参构造与字段名。Gson 自带的 `gson.pro` 仅覆盖库内部路径，不覆盖业务类。

编解码器为进程级配置，MMKV 与 DataStore 两个后端共用同一份。

### 约定

- 同一 key 必须使用同一类型读写。存储层不记录值的类型，类型不符时的读取结果未定义。
- `putKV(key, null)` 表示删除该键，而非写入 null。

---

[返回根目录 README](../../README.md)
