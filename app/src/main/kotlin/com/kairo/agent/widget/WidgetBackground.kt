package com.kairo.agent.widget

import android.content.Context
import android.widget.RemoteViews
import com.kairo.agent.R

/**
 * Request wallpaper blur from One UI Home using its widget background contract.
 * The host renders the blur; custom Views and RenderEffect cannot do this in RemoteViews.
 * Other Samsung hosts may show only the tint. Do not assume cover/keyguard blur support.
 */
internal fun RemoteViews.applyWidgetBackground(context: Context) {
    if (context.packageManager.hasSystemFeature("com.samsung.feature.samsung_experience_mobile")) {
        // A ColorDrawable with alpha 1..254 is required by the Samsung host.
        setInt(android.R.id.background, "setBackgroundColor", 0x9912141A.toInt())
    } else {
        setInt(android.R.id.background, "setBackgroundResource", R.drawable.widget_bg_dark)
    }
}
