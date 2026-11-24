# gson: malformed JSON repair

English | [中文](gson.md)

Module `:gson`, depends on Gson.

Coordinates and repository setup: [root README](../../README_EN.md#getting-started).

JSON returned by servers is damaged in three common ways. The repairer handles each class and then retries with pairwise and three-way combinations:

| Damage | Example | Handling |
|---|---|---|
| Invalid escape | `"path":"C://Users//name"` | Invalid `\` is escaped to `\\`; an incomplete `\u` degrades to a plain backslash |
| Unescaped quote | `"desc":"he said "done" and left"` | Skip whitespace and inspect the next character; anything other than `, } ] :` marks a content quote and is escaped |
| Truncation | `{"a":1,"b":` | Closing symbols are appended in reverse container order; a dangling member falls back to the previous comma and retries |

```kotlin
val result = JsonRepair.repair(raw) ?: return
if (!result.isUnmodified) {
    log("applied: ${result.applied.joinToString { it.name }}")
}
parse(result.text)
```

`applied` records which repair classes were used, which is useful for measuring the distribution of malformed payloads in production.

### API

| Call | Description |
|---|---|
| `JsonRepair.repair(raw)` | Runs the strategy chain, returns `Result?`; `null` means it gave up |
| `JsonRepair.escapeInvalidSequences` / `escapeUnescapedQuotes` / `closeTruncated` | Single repairs; return `null` when nothing needs repairing |
| `JsonObject.stringOr / intOr / longOr / doubleOr / floatOr / booleanOr` | Safe access, falling back to a default on type mismatch |
| `JsonObject.objectOrNull / arrayOrNull / elementOrNull` | Structural access, distinguishing absence from an explicit `null` |
| `String?.toJsonObjectOrNull()` / `toListOrNull<T>()` / `toObjectOrNull<T>()` | Parse entry points, returning `null` on failure |
| `Any?.toJsonOrNull()` | Serialization with HTML escaping disabled |

Missing fields, `null` values and type mismatches never throw:

```kotlin
val name = json.stringOr("name")     // "" when missing or null
val count = json.intOr("count", -1)  // non-numeric strings also fall back
val tags = json.arrayOrNull("tags")  // null on type mismatch, no ClassCastException
```

### Limitations

- The goal is text that a standard parser accepts, with no semantic inference: structures missing a value such as `{"a":,"b":1}` are not patched and yield `null`.
- Valid escapes are left untouched, so `\n` and `\t` inside Windows paths still parse as newline and tab. Such content has to be escaped at the protocol level.

## FAQ

### Does the repairer affect valid JSON

No. The first step is strict validation, and valid text is returned unchanged with an empty `applied` list. Every repair candidate has to pass the same validation or is discarded.

---

[Back to the root README](../../README_EN.md)
