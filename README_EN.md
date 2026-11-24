# OpenLibsExtensions

English | [中文](README.md)

Thin wrappers around Coil, Gson, Moshi, Lottie, ZXing, MMKV / DataStore and WorkManager, split into independent Gradle modules: gradient blur, malformed-JSON repair, shared Lottie composition cache, QR encoding, key-value storage, a coroutine task index and a trial-range seek bar.

Each module addresses a problem that recurs in production: no GPU blur path below API 33, servers occasionally returning damaged JSON, dozens of Lottie instances each parsing their own animation, QR codes losing readability once a centre icon is drawn over them. Apart from `:kv-gson`, which depends on `:kv`, the modules are independent and can be adopted separately.

## Table of contents

- [Capabilities](#capabilities)
- [Compatibility](#compatibility)
- [Dependency contract](#dependency-contract)
- [Modules](#modules)
- [Getting started](#getting-started)
- [Compose](#compose)
- [Related documentation](#related-documentation)
- [License](#license)

## Capabilities

- **Small dependency surface**: `:gson` and `:moshi` are pure Kotlin/JVM modules usable outside Android. The remaining modules depend only on the library they wrap, pulling in no image or camera dependencies.
- **Two paths, one geometry**: blur ships GPU (AGSL) and CPU (StackBlur) implementations. The radius is expressed as a fraction of height, so one configuration renders consistently across differently sized views.
- **Graceful degradation**: APIs fall back automatically when unavailable, leaving call sites free of version branching.

## Compatibility

| Item | Requirement |
|---|---|
| minSdk | 21 |
| compileSdk | 37 |
| Host `jvmTarget` | `:coil` ≥ **11**, other modules ≥ **1.8** |
| AGP | 9.5.1 (built-in Kotlin, no `kotlin-android` plugin) |
| Gradle | 9.6.0 |
| Kotlin | 2.4.21 |
| JDK | 17+ |
| Blur GPU path | API 33+ (`RuntimeShader`), CPU fallback below |

Where the `jvmTarget` floor comes from: public APIs contain `inline` functions (`toListOrNull<T>()`, `toKey`, and others). Kotlin refuses to inline bytecode built for a newer target than the consumer's, so compiling against this library fails when the host target is lower than the value above. The floor of 11 for `:coil` is imposed by Coil 3, whose bytecode targets JVM 11 and whose `ImageView.load` is itself an `inline` function.

## Dependency contract

Artifacts are thin AAR/JAR files. Nothing is shaded and no fat-jar is produced: third-party libraries are declared as Gradle dependencies, and the resolved version is decided by the host's conflict resolution.

| Module | Compile-time dependency | Declaration |
|---|---|---|
| `:coil` | `io.coil-kt.coil3:coil:3.6.3`, `androidx.annotation:1.11.0` | `requires` (floor) |
| `:lottie` | `com.airbnb.android:lottie:6.7.1`, `androidx.annotation:1.11.0` | `requires` (floor) |
| `:zxing` | `com.google.zxing:core:3.5.4` | `requires` (floor) |
| `:gson` | `com.google.code.gson:gson:2.15.0` | `requires` (floor) |
| `:moshi` | `com.squareup.moshi:moshi:1.15.2` | `requires` (floor) |
| `:kv` | `com.tencent:mmkv:2.4.3`, `androidx.datastore:datastore-preferences:1.3.0`, `org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0` | `requires` (floor) |
| `:kv-gson` | `:kv`, `com.google.code.gson:gson:2.15.0` | `requires` (floor) |
| `:coroutines` | `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0` | `requires` (floor) |
| `:workmanager` | `androidx.work:work-runtime:2.12.0`, `androidx.annotation:1.11.0` | `requires` (floor) |
| `:trial` | no third-party dependency | — |
| `:trial-compose` | `:trial`, Compose BOM 2026.09.00 | `requires` (floor) |

`requires` states a floor rather than a pin: when the host asks for a newer version Gradle resolves the higher one, and the library never downgrades it; when the host asks for an older version it is raised to the value above.

`kotlin-stdlib` is raised to 2.4.21, so the host Kotlin compiler must be 2.x. This follows from the language version the library is built with and cannot be avoided through dependency declarations.

The only transitive androidx dependency is `annotation`, used by `@DrawableRes` / `@RawRes` / `@RequiresApi` in public signatures. `androidx.core` is not pulled in.

To stay on a lower version, exclude the dependency and provide it explicitly:

```kotlin
implementation("com.github.git-a-live.openlibsextensions:coil:v0.1.6") {
    exclude(group = "io.coil-kt.coil3", module = "coil")
}
implementation("io.coil-kt.coil3:coil:3.2.0")
```

Binary compatibility is not guaranteed below the versions listed above: superclasses and method signatures such as `Transformation` or `LottieDrawable` may be missing or have changed semantics.

## Modules

| Module | Depends on | Provides | Documentation |
|---|---|---|---|
| `:coil` | Coil 3.x | Top/bottom gradient blur, colour overlay, GPU (AGSL) and CPU (StackBlur) paths | [docs](docs/modules/coil_EN.md) |
| `:gson` | Gson | Null-safe accessors, three classes of malformed-JSON repair (invalid escapes / unescaped quotes / truncation) | [docs](docs/modules/gson_EN.md) |
| `:moshi` | Moshi | Serialization entry points and accessors for the untyped map model | [docs](docs/modules/moshi_EN.md) |
| `:lottie` | Lottie | Compositions shared by `resource id + night mode`, played on demand and released on recycle | [docs](docs/modules/lottie_EN.md) |
| `:zxing` | ZXing core | QR generation with a configurable centre icon and error-correction level | [docs](docs/modules/zxing_EN.md) |
| `:kv` | MMKV / DataStore / kotlinx.serialization | Generic infix key-value access, dispatching on the runtime type | [docs](docs/modules/kv_EN.md) |
| `:kv-gson` | `:kv` + Gson | Optional codec for classes without `@Serializable` | [docs](docs/modules/kv_EN.md) |
| `:coroutines` | kotlinx-coroutines | Task index keyed by tag, entries removed on completion | [docs](docs/modules/coroutines_EN.md) |
| `:workmanager` | WorkManager | Request and constraint construction, worker type inferred from the call site | [docs](docs/modules/workmanager_EN.md) |
| `:trial` | none | Trial-range seek bar (View) with configurable colours and endpoint style | [docs](docs/modules/trial_EN.md) |
| `:trial-compose` | `:trial` + Compose | The same control for Compose | [docs](docs/modules/trial_EN.md) |

Each module document also has a Chinese version next to the English file.

## Getting started

### 1. Requirements

- `minSdk` 21 or higher
- `compileSdk` 37 or higher (the AAR metadata declares `minCompileSdk=37`; older AGP versions fail immediately)
- JDK 17 or higher

### 2. Add the repository

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            // The actual group is com.github.git-a-live.openlibsextensions: a multi-module
            // project appends the repository name to the prefix. includeGroup matches
            // exactly, so a regex covers both the prefix and the full group.
            content { includeGroupByRegex("com\\.github\\.git-a-live(\\..*)?") }
        }
    }
}
```

The group filter keeps JitPack out of unrelated dependency resolution. Omitting it works as well, at the cost of extra requests to JitPack during sync.

### 3. Add dependencies

Pick the modules you need:

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.git-a-live.openlibsextensions:coil:v0.1.6")    // gradient blur
    implementation("com.github.git-a-live.openlibsextensions:lottie:v0.1.6")  // shared composition cache
    implementation("com.github.git-a-live.openlibsextensions:zxing:v0.1.6")   // QR encoding
    implementation("com.github.git-a-live.openlibsextensions:kv:v0.1.6")      // key-value storage
    implementation("com.github.git-a-live.openlibsextensions:kv-gson:v0.1.6") // optional: plain classes
    implementation("com.github.git-a-live.openlibsextensions:gson:v0.1.6")    // malformed-JSON repair
    implementation("com.github.git-a-live.openlibsextensions:moshi:v0.1.6")   // Moshi serialization
    implementation("com.github.git-a-live.openlibsextensions:coroutines:v0.1.6")   // task index
    implementation("com.github.git-a-live.openlibsextensions:workmanager:v0.1.6")  // WorkManager requests
    implementation("com.github.git-a-live.openlibsextensions:trial:v0.1.6")        // trial-range seek bar
    implementation("com.github.git-a-live.openlibsextensions:trial-compose:v0.1.6") // same, for Compose
}
```

Coordinates follow `com.github.git-a-live.openlibsextensions:module:version`.

| Version form | Meaning | Use |
|---|---|---|
| `v0.1.6` | Release tag | Production |
| `a1b2c3d` | Short commit hash | Verifying a single fix |
| `main-SNAPSHOT` | Latest commit on the branch | Tracking development |

Coordinates are written in lower case. JitPack paths are case-insensitive (`com.github.philjay/mpandroidchart` and `com.github.PhilJay/MPAndroidChart` return the same content), while the Maven convention requires lower case; this document uses lower case throughout.

### 4. Troubleshooting

| Symptom | Cause and remedy |
|---|---|
| Resolution returns 404 or hangs | JitPack builds a tag on first request, usually within 1–3 minutes. Build log: `https://jitpack.io/com/github/git-a-live/openlibsextensions/v0.1.6/build.log` |
| A new tag resolves to old content | JitPack caches builds; re-resolve with `--refresh-dependencies` |
| `Dependency requires compileSdk 37` | The host `compileSdk` is below 37 |
| `coil3.transform.Transformation` not found | Another Coil version is taking precedence; check `./gradlew :app:dependencies` for the resolved version |

## Compose

`GradientBlurTransformation` is a standard Coil `Transformation` and works with `AsyncImage`; `GradientBlurEffect` (AGSL) is a View-layer implementation. `:trial-compose` provides the trial-range seek bar for Compose. The remaining modules are independent of any UI toolkit.

## Related documentation

- [docs/modules/](docs/modules/): usage, API and limitations for each module
- [docs/DEVELOPING.md](docs/DEVELOPING.md): project layout, build and tests, coding conventions, design trade-offs
- [docs/PUBLISHING.md](docs/PUBLISHING.md): coordinates, tagging, JitPack build mechanics

## License

[MIT](LICENSE)
