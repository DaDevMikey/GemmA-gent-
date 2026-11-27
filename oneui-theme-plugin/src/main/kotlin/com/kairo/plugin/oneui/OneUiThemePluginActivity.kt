package com.kairo.plugin.oneui

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class OneUiThemePluginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = "Kairo One UI Design Theme Plugin\n\nInspired by tribalfs/oneui-design.\nOpen Kairo → Settings → Plugins & Extensions to enable this theme."
            setPadding(48, 96, 48, 48)
            textSize = 16f
        }
        setContentView(tv)
    }
}
