package com.kairo.agent.data

import android.content.Context

enum class EngineBackendMode {
    AUTO,
    AICORE,
    LOCAL_HTTP
}

data class AgentSettings(
    val bypassCompatibility: Boolean = false,
    val rootPowerMode: Boolean = false,
    val showNerdStats: Boolean = true,
    val miniModeOverlay: Boolean = false,
    val backendMode: EngineBackendMode = EngineBackendMode.AUTO,
    val localEndpointUrl: String = "http://127.0.0.1:8080/v1/chat/completions",
    val localModelName: String = "gemma-4"
)

/**
 * Persists power-user, compatibility bypass, and local model backend settings.
 */
class AgentSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("kairo_settings", Context.MODE_PRIVATE)

    fun load(): AgentSettings = AgentSettings(
        bypassCompatibility = prefs.getBoolean(KEY_BYPASS, false),
        rootPowerMode = prefs.getBoolean(KEY_ROOT_POWER, false),
        showNerdStats = prefs.getBoolean(KEY_NERD_STATS, true),
        miniModeOverlay = prefs.getBoolean(KEY_MINI_MODE, false),
        backendMode = runCatching {
            EngineBackendMode.valueOf(prefs.getString(KEY_BACKEND, EngineBackendMode.AUTO.name) ?: EngineBackendMode.AUTO.name)
        }.getOrDefault(EngineBackendMode.AUTO),
        localEndpointUrl = prefs.getString(KEY_LOCAL_URL, "http://127.0.0.1:8080/v1/chat/completions")
            ?: "http://127.0.0.1:8080/v1/chat/completions",
        localModelName = prefs.getString(KEY_LOCAL_MODEL, "gemma-4") ?: "gemma-4"
    )

    fun save(settings: AgentSettings) {
        prefs.edit()
            .putBoolean(KEY_BYPASS, settings.bypassCompatibility)
            .putBoolean(KEY_ROOT_POWER, settings.rootPowerMode)
            .putBoolean(KEY_NERD_STATS, settings.showNerdStats)
            .putBoolean(KEY_MINI_MODE, settings.miniModeOverlay)
            .putString(KEY_BACKEND, settings.backendMode.name)
            .putString(KEY_LOCAL_URL, settings.localEndpointUrl)
            .putString(KEY_LOCAL_MODEL, settings.localModelName)
            .apply()
    }

    private companion object {
        const val KEY_BYPASS = "bypass_compatibility"
        const val KEY_ROOT_POWER = "root_power_mode"
        const val KEY_NERD_STATS = "show_nerd_stats"
        const val KEY_MINI_MODE = "mini_mode_overlay"
        const val KEY_BACKEND = "backend_mode"
        const val KEY_LOCAL_URL = "local_endpoint_url"
        const val KEY_LOCAL_MODEL = "local_model_name"
    }
}
