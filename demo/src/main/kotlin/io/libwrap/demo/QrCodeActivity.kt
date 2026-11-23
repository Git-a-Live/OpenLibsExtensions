package io.libwrap.demo

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import io.libwrap.zxing.QrCodes

class QrCodeActivity : Activity() {

    private lateinit var preview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr)

        val input = findViewById<EditText>(R.id.edit_content)
        preview = findViewById(R.id.image_qr)

        findViewById<Button>(R.id.button_generate).setOnClickListener {
            render(input.text.toString())
        }

        render(input.text.toString())
    }

    private fun render(content: String) {
        preview.setImageBitmap(QrCodes.encode(content.ifEmpty { "libwrap" }))
    }
}
