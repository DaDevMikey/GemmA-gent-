package com.kairo.agent.data

import android.content.Context

enum class EngineBackendMode {
    AUTO,
    AICORE,
    LOCAL_HTTP,
    CLOUD_AI
}

enum class AiCoreModelProfile(val label: String) {
    AUTO("Auto (Best Available)"),
    STABLE_FULL("Stable • Full"),
    STABLE_FAST("Stable • Fast"),
    PREVIEW_FULL("Preview • Full"),
    PREVIEW_FAST("Preview • Fast")
}

data class AgentSettings(
    val hasCompletedOnboarding: Boolean = false,
    val bypassCompatibility: Boolean = false,
    val rootPowerMode: Boolean = false,
    val showNerdStats: Boolean = true,
    val miniModeOverlay: Boolean = false,
    val backendMode: EngineBackendMode = EngineBackendMode.AUTO,
    val aiCoreModelProfile: AiCoreModelProfile = AiCoreModelProfile.AUTO,
    val localEndpointUrl: String = "http://127.0.0.1:8080/v1/chat/completions",
    val localModelName: String = "gemma-4",
    val cloudEndpointUrl: String = "https://api.openai.com/v1/chat/completions",
    val cloudApiKey: String = "",
    val cloudModelName: String = "gpt-4o-mini",
    val mcpServerUrls: String = ""
)

/**
 * Persists onboarding completion, power-user, compatibility bypass, and local model backend settings.
 */
class AgentSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("kairo_settings", Context.MODE_PRIVATE)

    fun load(): AgentSettings = AgentSettings(
        hasCompletedOnboarding = prefs.getBoolean(KEY_ONBOARDING, false),
        bypassCompatibility = prefs.getBoolean(KEY_BYPASS, false),
        rootPowerMode = prefs.getBoolean(KEY_ROOT_POWER, false),
        showNerdStats = prefs.getBoolean(KEY_NERD_STATS, true),
        miniModeOverlay = prefs.getBoolean(KEY_MINI_MODE, false),
        backendMode = runCatching {
            EngineBackendMode.valueOf(prefs.getString(KEY_BACKEND, EngineBackendMode.AUTO.name) ?: EngineBackendMode.AUTO.name)
        }.getOrDefault(EngineBackendMode.AUTO),
        aiCoreModelProfile = runCatching {
            AiCoreModelProfile.valueOf(prefs.getString(KEY_AICORE_PROFILE, AiCoreModelProfile.AUTO.name) ?: AiCoreModelProfile.AUTO.name)
        }.getOrDefault(AiCoreModelProfile.AUTO),
        localEndpointUrl = prefs.getString(KEY_LOCAL_URL, "http://127.0.0.1:8080/v1/chat/completions")
            ?: "http://127.0.0.1:8080/v1/chat/completions",
        localModelName = prefs.getString(KEY_LOCAL_MODEL, "gemma-4") ?: "gemma-4",
        cloudEndpointUrl = prefs.getString(KEY_CLOUD_URL, "https://api.openai.com/v1/chat/completions")
            ?: "https://api.openai.com/v1/chat/completions",
        cloudApiKey = prefs.getString(KEY_CLOUD_KEY, "") ?: "",
        cloudModelName = prefs.getString(KEY_CLOUD_MODEL, "gpt-4o-mini") ?: "gpt-4o-mini",
        mcpServerUrls = prefs.getString(KEY_MCP_URLS, "") ?: ""
    )

    fun save(settings: AgentSettings) {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING, settings.hasCompletedOnboarding)
            .putBoolean(KEY_BYPASS, settings.bypassCompatibility)
            .putBoolean(KEY_ROOT_POWER, settings.rootPowerMode)
            .putBoolean(KEY_NERD_STATS, settings.showNerdStats)
            .putBoolean(KEY_MINI_MODE, settings.miniModeOverlay)
            .putString(KEY_BACKEND, settings.backendMode.name)
            .putString(KEY_AICORE_PROFILE, settings.aiCoreModelProfile.name)
            .putString(KEY_LOCAL_URL, settings.localEndpointUrl)
            .putString(KEY_LOCAL_MODEL, settings.localModelName)
            .putString(KEY_CLOUD_URL, settings.cloudEndpointUrl)
            .putString(KEY_CLOUD_KEY, settings.cloudApiKey)
            .putString(KEY_CLOUD_MODEL, settings.cloudModelName)
            .putString(KEY_MCP_URLS, settings.mcpServerUrls)
            .apply()
    }

    private companion object {
        const val KEY_ONBOARDING = "has_completed_onboarding"
        const val KEY_BYPASS = "bypass_compatibility"
        const val KEY_ROOT_POWER = "root_power_mode"
        const val KEY_NERD_STATS = "show_nerd_stats"
        const val KEY_MINI_MODE = "mini_mode_overlay"
        const val KEY_BACKEND = "backend_mode"
        const val KEY_AICORE_PROFILE = "aicore_model_profile"
        const val KEY_LOCAL_URL = "local_endpoint_url"
        const val KEY_LOCAL_MODEL = "local_model_name"
        const val KEY_CLOUD_URL = "cloud_endpoint_url"
        const val KEY_CLOUD_KEY = "cloud_api_key"
        const val KEY_CLOUD_MODEL = "cloud_model_name"
        const val KEY_MCP_URLS = "mcp_server_urls"
    }
}
