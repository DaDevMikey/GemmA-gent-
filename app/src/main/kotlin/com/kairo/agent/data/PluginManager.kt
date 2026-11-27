package com.kairo.agent.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build

data class KairoPlugin(
    val packageName: String,
    val name: String,
    val description: String,
    val type: String, // "theme", "language", "tool"
    val accentColorHex: String? = null,
    val bgColorHex: String? = null,
    val surfaceColorHex: String? = null,
    val langCode: String? = null,
    val iconPack: String? = null,
    val cornerDp: Int? = null,
    val toolName: String? = null,
    val toolDesc: String? = null
) {
    fun accentColorInt(): Int? = accentColorHex?.let { runCatching { Color.parseColor(it) }.getOrNull() }
    fun bgColorInt(): Int? = bgColorHex?.let { runCatching { Color.parseColor(it) }.getOrNull() }
    fun surfaceColorInt(): Int? = surfaceColorHex?.let { runCatching { Color.parseColor(it) }.getOrNull() }
}

/**
 * Discovers standalone Kairo plugin APKs via PackageManager intent queries
 * (`com.kairo.agent.PLUGIN`) without requiring plugins to be launchable.
 */
object PluginManager {
    const val ACTION_KAIRO_PLUGIN = "com.kairo.agent.PLUGIN"
    private const val PREFS = "kairo_plugins"
    private const val KEY_ENABLED_THEME = "enabled_theme_pkg"
    private const val KEY_ENABLED_LANG_PREFIX = "enabled_lang_"

    fun discoverPlugins(context: Context): List<KairoPlugin> {
        val pm = context.packageManager
        val intent = Intent(ACTION_KAIRO_PLUGIN)
        val services = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
        }
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
        }
        val fromServices = services.mapNotNull { info ->
            val svc = info.serviceInfo ?: return@mapNotNull null
            val meta = svc.metaData ?: return@mapNotNull null
            val type = meta.getString("kairo.plugin.type") ?: return@mapNotNull null
            KairoPlugin(
                packageName = svc.packageName,
                name = meta.getString("kairo.plugin.name") ?: svc.loadLabel(pm).toString(),
                description = meta.getString("kairo.plugin.description") ?: "Kairo $type extension",
                type = type,
                accentColorHex = meta.getString("kairo.plugin.accent_color"),
                bgColorHex = meta.getString("kairo.plugin.bg_color"),
                surfaceColorHex = meta.getString("kairo.plugin.surface_color"),
                langCode = meta.getString("kairo.plugin.lang_code"),
                iconPack = meta.getString("kairo.plugin.icon_pack"),
                cornerDp = meta.getInt("kairo.plugin.corner_dp", 0).takeIf { it > 0 },
                toolName = meta.getString("kairo.plugin.tool_name"),
                toolDesc = meta.getString("kairo.plugin.tool_desc")
            )
        }
        val fromActivities = activities.mapNotNull { info ->
            val act = info.activityInfo ?: return@mapNotNull null
            val meta = act.metaData ?: return@mapNotNull null
            val type = meta.getString("kairo.plugin.type") ?: return@mapNotNull null
            KairoPlugin(
                packageName = act.packageName,
                name = meta.getString("kairo.plugin.name") ?: act.loadLabel(pm).toString(),
                description = meta.getString("kairo.plugin.description") ?: "Kairo $type extension",
                type = type,
                accentColorHex = meta.getString("kairo.plugin.accent_color"),
                bgColorHex = meta.getString("kairo.plugin.bg_color"),
                surfaceColorHex = meta.getString("kairo.plugin.surface_color"),
                langCode = meta.getString("kairo.plugin.lang_code"),
                iconPack = meta.getString("kairo.plugin.icon_pack"),
                cornerDp = meta.getInt("kairo.plugin.corner_dp", 0).takeIf { it > 0 },
                toolName = meta.getString("kairo.plugin.tool_name"),
                toolDesc = meta.getString("kairo.plugin.tool_desc")
            )
        }
        return (fromServices + fromActivities).distinctBy { it.packageName }
    }

    fun getEnabledThemePackage(context: Context): String? =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ENABLED_THEME, null)

    fun setEnabledThemePackage(context: Context, packageName: String?) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENABLED_THEME, packageName)
            .apply()
    }

    fun isPluginEnabled(context: Context, plugin: KairoPlugin): Boolean =
        if (plugin.type == "theme") getEnabledThemePackage(context) == plugin.packageName
        else context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("$KEY_ENABLED_LANG_PREFIX${plugin.packageName}", false)

    fun setPluginEnabled(context: Context, plugin: KairoPlugin, enabled: Boolean) {
        if (plugin.type == "theme") {
            setEnabledThemePackage(context, if (enabled) plugin.packageName else null)
        } else {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("$KEY_ENABLED_LANG_PREFIX${plugin.packageName}", enabled)
                .apply()
        }
    }

    fun getActiveThemePlugin(context: Context): KairoPlugin? {
        val pkg = getEnabledThemePackage(context) ?: return null
        return discoverPlugins(context).firstOrNull { it.packageName == pkg && it.type == "theme" }
    }
}
