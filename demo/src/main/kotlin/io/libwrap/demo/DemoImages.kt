package io.libwrap.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader

/**
 * demo 用的示例图：全部由代码绘制，不引入二进制资源，保证仓库干净且离线可跑。
 *
 * 刻意做成高对比的渐变加几何块——模糊效果在低频区域几乎看不出来，
 * 有明确边界和高光点才能直观比较模糊半径与过渡带。
 */
internal object DemoImages {

    fun cover(width: Int = 900, height: Int = 900): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawRect(
            0f, 0f, width.toFloat(), height.toFloat(),
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.parseColor("#1B2A4A"), Color.parseColor("#7A3E9D"), Color.parseColor("#E86A5B")),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP,
                )
            },
        )

        // 散布的高光圆点，用来观察模糊后的扩散半径
        val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        repeat(26) { index ->
            dots.alpha = 40 + (index % 5) * 25
            val x = (index * 137 % width).toFloat()
            val y = (index * 311 % height).toFloat()
            canvas.drawCircle(x, y, 12f + (index % 7) * 9f, dots)
        }

        // 几条硬边竖条，用来判断模糊是否被正确限制在指定区域
        val bars = Paint().apply { color = Color.parseColor("#F2E9D8") }
        for (i in 0 until 5) {
            val left = width * (0.08f + i * 0.18f)
            canvas.drawRect(left, height * 0.46f, left + width * 0.05f, height * 0.62f, bars)
        }

        return bitmap
    }
}
