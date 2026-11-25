package com.kairo.agent.engine

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import com.kairo.agent.service.NotificationCatchUpStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class CompatibilityCheckItem(
    val label: String,
    val detail: String,
    val passed: Boolean
)

data class CompatibilityReport(
    val items: List<CompatibilityCheckItem>,
    val aiCoreReady: Boolean,
    val localServerReady: Boolean,
    val healthConnectGranted: Boolean = false,
    val socAllowlisted: Boolean = false,
    val socName: String = ""
) {
    val allCorePassed: Boolean get() = aiCoreReady || localServerReady || socAllowlisted
}

/**
 * Checks device readiness (Android version, RAM, AICore, Local HTTP model endpoint,
 * permissions, and root availability) for the Setup & Compatibility sheet.
 */
class CompatibilityChecker(private val context: Context) {

    suspend fun runChecks(
        aiCoreAvailability: ModelAvailability,
        localEndpointUrl: String
    ): CompatibilityReport = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)

        val localReachable = probeLocalEndpoint(localEndpointUrl)
        val aiCoreOk = aiCoreAvailability == ModelAvailability.AVAILABLE
        val micOk = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val locationOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val contactsOk = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val notifOk = NotificationCatchUpStore.isAccessGranted(context)
        val rootOk = isRootAvailable()
        val healthOk = checkHealthConnectGranted()
        val socInfo = detectSocInfo()
        val socOk = isSocAllowlisted(socInfo)

        val items = listOf(
            CompatibilityCheckItem(
                label = "SoC / NPU Allowlist",
                detail = if (socOk) "$socInfo (Verified flagship NPU)" else "$socInfo (Not on flagship allowlist)",
                passed = socOk
            ),
            CompatibilityCheckItem(
                label = "Android OS & RAM",
                detail = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) • ${"%.1f".format(totalRamGb)} GB RAM",
                passed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ),
            CompatibilityCheckItem(
                label = "AICore (Gemma 4)",
                detail = when (aiCoreAvailability) {
                    ModelAvailability.AVAILABLE -> "Available on-device"
                    ModelAvailability.DOWNLOADABLE -> "Model downloadable in AICore"
                    ModelAvailability.DOWNLOADING -> "Downloading..."
                    else -> "Not available (use Local Endpoint or Bypass)"
                },
                passed = aiCoreOk
            ),
            CompatibilityCheckItem(
                label = "Local Model Endpoint",
                detail = if (localReachable) "Reachable at $localEndpointUrl" else "Not running at $localEndpointUrl",
                passed = localReachable
            ),
            CompatibilityCheckItem(
                label = "Voice & Mic Permission",
                detail = if (micOk) "Granted" else "Not granted",
                passed = micOk
            ),
            CompatibilityCheckItem(
                label = "Location (Local Context)",
                detail = if (locationOk) "Granted" else "Not granted",
                passed = locationOk
            ),
            CompatibilityCheckItem(
                label = "Contacts (Call / Text)",
                detail = if (contactsOk) "Granted" else "Not granted",
                passed = contactsOk
            ),
            CompatibilityCheckItem(
                label = "Notification Catch-Up",
                detail = if (notifOk) "Enabled" else "Not enabled",
                passed = notifOk
            ),
            CompatibilityCheckItem(
                label = "Root / Shell Power Mode",
                detail = if (rootOk) "su binary detected" else "Standard non-root shell",
                passed = rootOk
            )
        )

        CompatibilityReport(
            items = items,
            aiCoreReady = aiCoreOk,
            localServerReady = localReachable,
            healthConnectGranted = healthOk,
            socAllowlisted = socOk,
            socName = socInfo
        )
    }

    private fun detectSocInfo(): String {
        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
        } else null
        return listOfNotNull(socModel, Build.HARDWARE, Build.BOARD)
            .distinct()
            .joinToString(" / ")
            .ifBlank { "Unknown SoC" }
    }

    private fun isSocAllowlisted(socInfo: String): Boolean {
        val upper = socInfo.uppercase()
        val allowlist = listOf(
            "SM8750", // Snapdragon 8 Elite
            "SM8850", // Snapdragon 8 Elite 2 / Gen 5
            "SM8950", // Future 8-series
            "8 ELITE",
            "SUN", // SM8750 board/platform codename
            "TENSOR G5",
            "TENSOR G6",
            "LAGUNA", // Tensor G5 codename
            "MALIBU",
            "EXYNOS 2500",
            "EXYNOS2500",
            "S5E9955", // Exynos 2500
            "EXYNOS 2600",
            "EXYNOS2600",
            "S5E9965" // Exynos 2600
        )
        return allowlist.any { upper.contains(it) }
    }

    private suspend fun checkHealthConnectGranted(): Boolean {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return false
        return runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            HealthPermission.getReadPermission(StepsRecord::class) in granted ||
                HealthPermission.getReadPermission(HeartRateRecord::class) in granted
        }.getOrDefault(false)
    }

    private fun probeLocalEndpoint(endpointUrl: String): Boolean {
        return runCatching {
            val base = endpointUrl.substringBefore("/v1/").ifBlank { endpointUrl }
            val conn = (URL(base).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 900
                readTimeout = 900
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..499
        }.getOrDefault(false)
    }

    private fun isRootAvailable(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su")
        return paths.any { java.io.File(it).exists() }
    }
}
