package com.kairo.agent.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.kairo.agent.R
import com.kairo.agent.data.PluginManager

internal fun RemoteViews.applyWidgetIcons(context: Context, cover: Boolean) {
    val oneUi = PluginManager.getActiveThemePlugin(context)?.usesSesl9Controls == true
    setImageViewResource(if (cover) R.id.cover_widget_icon else R.id.widget_icon,
        if (oneUi) R.drawable.widget_oneui_assistant else R.mipmap.ic_launcher_round)
    setImageViewResource(if (cover) R.id.cover_widget_mic else R.id.widget_voice_btn,
        if (oneUi) R.drawable.oneui_ic_foldable_voice else android.R.drawable.ic_btn_speak_now)
}

/** Apply theme selection immediately to both widget variants, without waiting for a host refresh. */
internal fun refreshKairoWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val home = manager.getAppWidgetIds(ComponentName(context, KairoWidgetProvider::class.java))
    val cover = manager.getAppWidgetIds(ComponentName(context, KairoCoverWidgetProvider::class.java))
    if (home.isNotEmpty()) KairoWidgetProvider().onUpdate(context, manager, home)
    if (cover.isNotEmpty()) KairoCoverWidgetProvider().onUpdate(context, manager, cover)
}
