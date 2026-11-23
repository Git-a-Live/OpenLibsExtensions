package io.libwrap.gson

/**
 * 对"接近合法但被污染"的 JSON 文本做结构化修复。
 *
 * 面向的是服务端返回脏数据的场景：字段值里的引号没转义、反斜杠用错、响应在网络层被截断。
 * 三类损伤相互独立，也可能同时出现，因此按"单策略 → 双策略 → 三策略"的顺序逐级尝试，
 * 命中的策略会随结果一并返回，便于调用方埋点统计哪一类脏数据最常见。
 *
 * 每次修复都会用 [JsonSyntax] 复核，复核不通过的候选直接丢弃，不会把更坏的结果交给调用方。
 * 该类无状态且线程安全。
 */
public object JsonRepair {

    /** 单类修复手段。 */
    public enum class Fix {
        /** 字符串值内非法的反斜杠转义（如 `\ `、`\x`、残缺的 `\u`） */
        ESCAPES,

        /** 字符串值内未转义的双引号 */
        UNESCAPED_QUOTES,

        /** 尾部截断：字符串未闭合、括号/大括号未配对 */
        TRUNCATION,
    }

    /** 修复结果。[applied] 为空表示原文本来就合法，未做任何改动。 */
    public data class Result(
        val text: String,
        val applied: List<Fix>,
    ) {
        public val isUnmodified: Boolean get() = applied.isEmpty()
    }

    /** 尝试顺序：先单策略，再两两组合，最后三策略全上。顺序即优先级。 */
    private val strategies: List<List<Fix>> = listOf(
        listOf(Fix.ESCAPES),
        listOf(Fix.UNESCAPED_QUOTES),
        listOf(Fix.TRUNCATION),
        listOf(Fix.ESCAPES, Fix.UNESCAPED_QUOTES),
        listOf(Fix.UNESCAPED_QUOTES, Fix.TRUNCATION),
        listOf(Fix.ESCAPES, Fix.TRUNCATION),
        listOf(Fix.ESCAPES, Fix.UNESCAPED_QUOTES, Fix.TRUNCATION),
    )

    /**
     * 修复 [raw]。
     *
     * @return 修复成功（或原文本就合法）的结果；所有策略均失败时返回 null，调用方应放弃解析。
     */
    public fun repair(raw: String): Result? {
        if (JsonSyntax.isValid(raw)) return Result(raw, emptyList())

        for (strategy in strategies) {
            var candidate: String? = raw
            for (fix in strategy) {
                candidate = candidate?.let { applyFix(fix, it) }
                if (candidate == null) break
            }
            if (candidate != null && JsonSyntax.isValid(candidate)) {
                return Result(candidate, strategy)
            }
        }
        return null
    }

    /**
     * 修复字符串内非法的反斜杠转义：`\` 后面跟着不属于 `"\/bfnrtu` 的字符时，
     * 把该 `\` 自身转义为 `\\`；`\u` 后面不足 4 位十六进制时同样按普通反斜杠处理。
     *
     * @return 修复后的文本；无需修复时返回 null
     */
    public fun escapeInvalidSequences(raw: String): String? {
        val out = StringBuilder(raw.length + 16)
        var inString = false
        var changed = false
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (!inString) {
                out.append(c)
                if (c == '"') inString = true
                i++
                continue
            }
            if (c != '\\') {
                out.append(c)
                i++
                continue
            }
            // 字符串内的反斜杠：判断后续序列是否合法
            val next = raw.getOrNull(i + 1)
            when {
                next == null -> {
                    // 末尾孤立的反斜杠，交给截断修复处理，这里保持原样
                    out.append(c)
                    i++
                }
                next == 'u' -> {
                    val hasFourHex = i + 5 < raw.length &&
                        (2..5).all { raw[i + it].isHexDigit() }
                    if (hasFourHex) {
                        out.append(raw, i, i + 6)
                        i += 6
                    } else {
                        out.append("\\\\")
                        changed = true
                        i++
                    }
                }
                next.isValidEscapeChar() -> {
                    out.append(c).append(next)
                    i += 2
                }
                else -> {
                    out.append("\\\\")
                    changed = true
                    i++
                }
            }
        }
        return if (changed) out.toString() else null
    }

    /**
     * 修复字符串内未转义的双引号。
     *
     * 判定方式：遇到 `"` 时跳过后续空白看下一个字符，若为 `,` `}` `]` `:` 则认为是字符串
     * 正常结束，否则视为内容里的引号并补上转义。该启发式只适用于结构化 JSON，
     * 对纯文本片段（如单个未包裹的字符串）不适用。
     *
     * @return 修复后的文本；无需修复时返回 null
     */
    public fun escapeUnescapedQuotes(raw: String): String? {
        val out = StringBuilder(raw.length + 16)
        var inString = false
        var changed = false
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            when {
                !inString -> {
                    out.append(c)
                    if (c == '"') inString = true
                    i++
                }
                c == '\\' -> {
                    // 已有的转义序列整体透传
                    out.append(c)
                    val next = raw.getOrNull(i + 1)
                    if (next != null) {
                        out.append(next)
                        i += 2
                    } else {
                        i++
                    }
                }
                c == '"' -> {
                    if (closesStringAt(raw, i)) {
                        out.append(c)
                        inString = false
                    } else {
                        out.append("\\\"")
                        changed = true
                    }
                    i++
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return if (changed) out.toString() else null
    }

    /**
     * 补齐被截断的 JSON。
     *
     * 先按扫描结果补引号与括号；若补齐后仍不合法（典型情况是截断点落在 `"key":` 上，
     * 补完是 `{"a":1,"b":}`），则回退到上一个逗号处重试，直至合法或用尽尝试次数。
     * 末尾悬空的逗号会被直接丢弃。
     *
     * @return 修复后的文本；无法补齐时返回 null
     */
    public fun closeTruncated(raw: String): String? {
        var body = raw.trimEnd()
        if (body.isEmpty()) return null
        // 已合法时不处理，避免对正常文本做出改动
        if (JsonSyntax.isValid(body)) return null
        if (body.endsWith(",")) body = body.dropLast(1).trimEnd()

        repeat(MAX_TRUNCATION_ATTEMPTS) {
            val state = scanTailState(body)
            val closed = buildString(body.length + 8) {
                append(body)
                if (state.inString) append('"')
                append(state.closers)
            }
            if (JsonSyntax.isValid(closed)) return closed

            val cut = body.lastIndexOf(',')
            if (cut <= 0) return null
            body = body.substring(0, cut).trimEnd()
        }
        return null
    }

    private fun applyFix(fix: Fix, text: String): String? = when (fix) {
        Fix.ESCAPES -> escapeInvalidSequences(text)
        Fix.UNESCAPED_QUOTES -> escapeUnescapedQuotes(text)
        Fix.TRUNCATION -> closeTruncated(text)
    }

    /** 跳过空白后，该引号是否位于合法的结构边界上（即字符串结束）。 */
    private fun closesStringAt(raw: String, quoteIndex: Int): Boolean {
        var j = quoteIndex + 1
        while (j < raw.length && raw[j].isJsonWhitespace()) j++
        return when (raw.getOrNull(j)) {
            ',', '}', ']', ':' -> true
            // 文本到结尾即止：由截断修复负责补引号，这里按结束处理
            null -> true
            else -> false
        }
    }

    /**
     * 单次线性扫描：字符串是否仍未闭合，以及按嵌套顺序应当补上的闭合符号。
     *
     * 闭合符号必须按容器栈逆序生成——`{"a":[{"b":1` 需要补 `}]}`，
     * 而不是"先把所有 `]` 补完再补 `}`"。
     */
    private fun scanTailState(text: String): TailState {
        var inString = false
        val stack = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inString) {
                when (c) {
                    '\\' -> i += 2
                    '"' -> {
                        inString = false
                        i++
                    }
                    else -> i++
                }
            } else {
                when (c) {
                    '"' -> {
                        inString = true
                        i++
                    }
                    '{' -> {
                        stack.append('}')
                        i++
                    }
                    '[' -> {
                        stack.append(']')
                        i++
                    }
                    '}', ']' -> {
                        if (stack.isNotEmpty()) stack.setLength(stack.length - 1)
                        i++
                    }
                    else -> i++
                }
            }
        }
        return TailState(inString, stack.reverse().toString())
    }

    private data class TailState(val inString: Boolean, val closers: String)

    private const val MAX_TRUNCATION_ATTEMPTS = 8

    private fun Char.isValidEscapeChar(): Boolean =
        this == '"' || this == '\\' || this == '/' || this == 'b' ||
            this == 'f' || this == 'n' || this == 'r' || this == 't'

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private fun Char.isJsonWhitespace(): Boolean =
        this == ' ' || this == '\t' || this == '\n' || this == '\r'
}
