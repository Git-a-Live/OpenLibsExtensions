package io.libwrap.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_blur).setOnClickListener {
            startActivity(Intent(this, BlurActivity::class.java))
        }
        findViewById<Button>(R.id.button_json).setOnClickListener {
            startActivity(Intent(this, JsonRepairActivity::class.java))
        }
        findViewById<Button>(R.id.button_lottie).setOnClickListener {
            startActivity(Intent(this, LottieCacheActivity::class.java))
        }
        findViewById<Button>(R.id.button_qr).setOnClickListener {
            startActivity(Intent(this, QrCodeActivity::class.java))
        }
        findViewById<Button>(R.id.button_trial).setOnClickListener {
            startActivity(Intent(this, TrialSeekBarActivity::class.java))
        }
    }
}
