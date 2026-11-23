package io.libwrap.demo

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import io.libwrap.gson.JsonRepair
import io.libwrap.gson.toJsonObjectOrNull

/**
 * 逐个尝试内置的脏数据样例，展示修复命中的策略链以及修复后的文本。
 */
class JsonRepairActivity : Activity() {

    private val samples = listOf(
        """{"code":200,"path":"C:\Users\demo\name"}""",
        """{"title":"他说"你好"就走了","id":7}""",
        """{"list":[1,2,{"k":"v"}""",
        """{"a":1,"b":""",
        """{"desc":"a\zb"c","list":[1,2""",
    )

    private var index = 0

    private lateinit var input: EditText
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_json)

        input = findViewById(R.id.edit_input)
        output = findViewById(R.id.text_output)

        findViewById<Button>(R.id.button_next_sample).setOnClickListener {
            index = (index + 1) % samples.size
            show(samples[index])
        }
        findViewById<Button>(R.id.button_repair).setOnClickListener {
            repair(input.text.toString())
        }

        show(samples[index])
    }

    private fun show(sample: String) {
        input.setText(sample)
        input.setSelection(sample.length)
        repair(sample)
    }

    private fun repair(raw: String) {
        val result = JsonRepair.repair(raw)
        output.text = when {
            result == null -> "无法修复：三类策略均未产出合法 JSON"
            result.isUnmodified -> "原文已合法，未做改动\n\n${result.text}"
            else -> buildString {
                append("命中策略：")
                append(result.applied.joinToString(" → "))
                append("\n\n")
                append(result.text)
                append("\n\n可被 Gson 解析：")
                append(result.text.toJsonObjectOrNull() != null)
            }
        }
    }
}
