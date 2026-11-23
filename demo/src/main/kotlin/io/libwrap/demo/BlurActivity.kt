package io.libwrap.demo

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import io.libwrap.coil.BlurBand
import io.libwrap.coil.GradientBlurOptions
import io.libwrap.coil.loadWithGradientBlur
import io.libwrap.coil.releaseGradientBlur
import io.libwrap.coil.restoreGradientBlur

/**
 * 三张同样的图分别演示：顶部模糊带、底部模糊带 + 渐变色叠加、整图模糊。
 * 底部按钮可在 GPU（API 33+）与 CPU 路径之间切换，用于对比两者视觉是否一致。
 */
class BlurActivity : Activity() {

    private lateinit var cover: Bitmap
    private var preferGpu = true
    private var released = false

    private lateinit var topView: ImageView
    private lateinit var bottomView: ImageView
    private lateinit var fullView: ImageView
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blur)

        cover = DemoImages.cover()
        topView = findViewById(R.id.image_top)
        bottomView = findViewById(R.id.image_bottom)
        fullView = findViewById(R.id.image_full)
        statusView = findViewById(R.id.text_status)

        findViewById<Button>(R.id.button_toggle_path).setOnClickListener {
            preferGpu = !preferGpu
            released = false
            apply()
        }
        findViewById<Button>(R.id.button_release).setOnClickListener {
            if (released) {
                topView.restoreGradientBlur()
                bottomView.restoreGradientBlur()
                fullView.restoreGradientBlur()
                released = false
            } else {
                topView.releaseGradientBlur()
                bottomView.releaseGradientBlur()
                fullView.releaseGradientBlur()
                released = true
            }
            updateStatus()
        }

        apply()
    }

    private fun apply() {
        topView.loadWithGradientBlur(
            data = cover,
            options = GradientBlurOptions(
                blurRatio = 0.035f,
                top = BlurBand(coverage = 0.45f, fade = 0.18f),
                bottom = null,
                cornerRadiusPx = 28f,
                preferGpu = preferGpu,
            ),
        )

        bottomView.loadWithGradientBlur(
            data = cover,
            options = GradientBlurOptions(
                blurRatio = 0.04f,
                top = null,
                bottom = BlurBand(
                    coverage = 0.55f,
                    fade = 0.25f,
                    tintStart = 0x00FFFFFF,
                    tintEnd = 0xCC1B2A4A.toInt(),
                ),
                cornerRadiusPx = 28f,
                preferGpu = preferGpu,
            ),
        )

        fullView.loadWithGradientBlur(
            data = cover,
            options = GradientBlurOptions(
                blurRatio = 0.02f,
                top = BlurBand(coverage = 1f, fade = 0f),
                bottom = null,
                cornerRadiusPx = 28f,
                preferGpu = preferGpu,
            ),
        )

        released = false
        updateStatus()
    }

    private fun updateStatus() {
        val gpuReady = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        val actual = if (preferGpu && gpuReady) "GPU (AGSL)" else "CPU (StackBlur)"
        val releasedHint = if (released) " · 效果已释放" else ""
        statusView.text = "当前路径：$actual$releasedHint"
    }
}
