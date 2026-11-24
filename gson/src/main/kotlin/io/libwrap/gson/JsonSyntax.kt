package io.libwrap.gson

/**
 * 轻量 JSON 语法校验器：只判断文本是否为合法 JSON，不做反序列化。
 *
 * 修复流程需要在"试探性修复 → 校验"之间反复迭代，校验失败属于预期分支，
 * 用 Gson 的异常承担控制流会产生大量异常对象，因此这里单独实现一个不抛异常的扫描器。
 *
 * 校验口径比 Gson 默认的 lenient 模式更严格（RFC 8259）：拒绝前导零数字、单引号、
 * 注释、尾随逗号，字符串内拒绝未转义控制字符。偏严格是刻意的——修复的目标是让文本
 * 能被任意标准解析器接受。
 */
internal object JsonSyntax {

    private const val MAX_DEPTH = 512

    fun isValid(text: String): Boolean = Parser(text).parseDocument()

    private class Parser(private val source: String) {

        private var index = 0

        fun parseDocument(): Boolean {
            skipWhitespace()
            if (!parseValue(0)) return false
            skipWhitespace()
            return index == source.length
        }

        private fun parseValue(depth: Int): Boolean {
            if (depth > MAX_DEPTH) return false
            skipWhitespace()
            if (index >= source.length) return false
            return when (source[index]) {
                '{' -> parseObject(depth)
                '[' -> parseArray(depth)
                '"' -> parseString()
                't' -> expectLiteral("true")
                'f' -> expectLiteral("false")
                'n' -> expectLiteral("null")
                else -> parseNumber()
            }
        }

        private fun parseObject(depth: Int): Boolean {
            index++ // '{'
            skipWhitespace()
            if (peek() == '}') {
                index++
                return true
            }
            while (true) {
                skipWhitespace()
                if (peek() != '"') return false
                if (!parseString()) return false
                skipWhitespace()
                if (peek() != ':') return false
                index++
                if (!parseValue(depth + 1)) return false
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    '}' -> {
                        index++
                        return true
                    }
                    else -> return false
                }
            }
        }

        private fun parseArray(depth: Int): Boolean {
            index++ // '['
            skipWhitespace()
            if (peek() == ']') {
                index++
                return true
            }
            while (true) {
                if (!parseValue(depth + 1)) return false
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    ']' -> {
                        index++
                        return true
                    }
                    else -> return false
                }
            }
        }

        private fun parseString(): Boolean {
            index++ // opening quote
            while (index < source.length) {
                val c = source[index]
                when {
                    c == '"' -> {
                        index++
                        return true
                    }
                    c == '\\' -> if (!parseEscape()) return false
                    c < ' ' -> return false
                    else -> index++
                }
            }
            return false
        }

        private fun parseEscape(): Boolean {
            index++ // backslash
            if (index >= source.length) return false
            when (source[index]) {
                '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> {
                    index++
                    return true
                }
                'u' -> {
                    if (index + 4 >= source.length) return false
                    for (offset in 1..4) {
                        if (!isHexDigit(source[index + offset])) return false
                    }
                    index += 5
                    return true
                }
                else -> return false
            }
        }

        private fun parseNumber(): Boolean {
            val start = index
            if (peek() == '-') index++
            if (index >= source.length) return false
            when {
                peek() == '0' -> index++
                peek() in '1'..'9' -> while (index < source.length && source[index].isAsciiDigit()) index++
                else -> return false
            }
            if (peek() == '.') {
                index++
                if (index >= source.length || !source[index].isAsciiDigit()) return false
                while (index < source.length && source[index].isAsciiDigit()) index++
            }
            if (peek() == 'e' || peek() == 'E') {
                index++
                if (peek() == '+' || peek() == '-') index++
                if (index >= source.length || !source[index].isAsciiDigit()) return false
                while (index < source.length && source[index].isAsciiDigit()) index++
            }
            return index > start
        }

        private fun expectLiteral(literal: String): Boolean {
            if (!source.startsWith(literal, index)) return false
            index += literal.length
            return true
        }

        private fun skipWhitespace() {
            while (index < source.length && source[index].isJsonWhitespace()) index++
        }

        private fun peek(): Char = if (index < source.length) source[index] else '\u0000'
    }

    private fun Char.isJsonWhitespace(): Boolean = this == ' ' || this == '\t' || this == '\n' || this == '\r'

    private fun isHexDigit(c: Char): Boolean =
        c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
}
