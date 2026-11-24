# kv: key-value storage

English | [中文](kv.md)

Modules `:kv` and `:kv-gson`, depending on MMKV / DataStore / kotlinx.serialization.

Coordinates and repository setup: [root README](../../README_EN.md#getting-started).

Both backends share one type-dispatch rule: MMKV (synchronous) and DataStore (suspending).

```kotlin
// Initialise one of the two
Kv.init(context)              // MMKV backend, synchronous
DataStoreKv.init(dataStore)   // DataStore backend, suspending

42 toKey "age"                 // Int uses the native integer encoding
"Beijing" toKey "city"         // String stored as is
User("n", 1) toKey "user"      // everything else goes through the codec

val age: Int? = getKV("age")
val user: User? = getKV("user")
val city: String? = String::class fromKey "city"
```

### Backend differences

| Backend | Package | Write | Read |
|---|---|---|---|
| MMKV | `io.libwrap.kv` | synchronous | synchronous |
| DataStore | `io.libwrap.kv.datastore` | `suspend` | `suspend` |

The syntax is identical; importing `toKey` / `getKV` from the corresponding package switches backend. The DataStore variant has to be suspending: reads are built on `Flow<Preferences>` and writes are suspending transactions, with no synchronous read available. Emulating one with an in-memory snapshot makes a read immediately after a write return the previous value. MMKV reads through mmap and carries no such constraint.

### Type dispatch

Primitive types (String / Int / Long / Float / Double / Boolean) are dispatched on the runtime type and bypass the codec, so a value declared as `Any` is still stored as its real type:

```kotlin
val v: Any = 42
v toKey "k"          // stored as Int
getKV<Int>("k")      // 42
```

Composite types go through `KvCodec`, which receives the full type information from `typeOf<T>()`, including generic arguments.

### Serialization

The default implementation is kotlinx.serialization and accepts only `@Serializable` types:

```kotlin
@Serializable
data class User(val name: String, val age: Int)

User("n", 1) toKey "user"            // JSON string
listOf(User("a", 1)) toKey "users"   // generic collections

val user: User? = getKV("user")
val users: List<User>? = getKV("users")
```

Types without `@Serializable` are not written and no exception is raised. To support plain classes, add the optional module and replace the codec:

```kotlin
implementation("com.github.git-a-live.openlibsextensions:kv-gson:v0.1.6")

Kv.useCodec(GsonKvCodec())   // set once during initialisation

Plain(1, "a") toKey "plain"
val plain: Plain? = getKV("plain")
```

Gson serializes reflectively, so the host must keep no-arg constructors and field names for its data classes in the R8 configuration. The `gson.pro` shipped with Gson covers only the library's internal paths, not application classes.

The codec is process-wide: both MMKV and DataStore share the same instance.

### Contract

- A key must be read with the same type it was written with. The store does not record types, and reading with a mismatched type is undefined.
- `putKV(key, null)` deletes the key rather than writing a null.

---

[Back to the root README](../../README_EN.md)
