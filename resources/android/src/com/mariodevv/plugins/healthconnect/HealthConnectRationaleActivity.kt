package com.mariodevv.plugins.healthconnect

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Required by Android 14+: explains why the app reads Health Connect data. */
open class HealthConnectRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(TextView(this@HealthConnectRationaleActivity).apply {
                text = "This app reads the health data you grant access to from Health Connect. It stays on your device unless the app saves or syncs it."
                textSize = 18f
            })
            addView(Button(this@HealthConnectRationaleActivity).apply {
                text = "Close"
                setOnClickListener { finish() }
            })
        })
    }
}
