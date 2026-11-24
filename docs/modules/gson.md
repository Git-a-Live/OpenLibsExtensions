# gson：脏 JSON 容错

[English](gson_EN.md) | 中文

模块 `:gson`，依赖 Gson。

依赖坐标与仓库配置见[根目录 README](../../README.md#接入)。

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

## 常见问题

### 修复器对合法 JSON 的影响

无影响。第一步为严格校验，合法文本原样返回（`applied` 为空）；每个修复候选同样需通过校验，未通过则丢弃。

---

[返回根目录 README](../../README.md)
