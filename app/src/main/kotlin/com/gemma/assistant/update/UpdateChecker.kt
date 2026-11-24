package com.gemma.assistant.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateCheckResult {
    data class UpdateAvailable(
        val latestVersion: String,
        val releaseUrl: String,
        val apkUrl: String?,
        val releaseNotes: String
    ) : UpdateCheckResult

    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Unavailable(val reason: String) : UpdateCheckResult
}

/**
 * Watches GitHub Releases for newer versions of GemmA.
 */
class UpdateChecker(private val context: Context) {

    fun currentVersionName(): String {
        return runCatching {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pkgInfo.versionName ?: "1.0"
        }.getOrElse { "1.0" }
    }

    suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val current = currentVersionName()
        runCatching {
            val conn = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "GemmA-Android/$current")
            }

            val code = conn.responseCode
            if (code == 404) {
                return@withContext UpdateCheckResult.Unavailable(
                    "No public release found yet (repo may be private)."
                )
            }
            if (code !in 200..299) {
                return@withContext UpdateCheckResult.Unavailable("GitHub returned HTTP $code")
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.optString("tag_name").trim()
            val htmlUrl = json.optString("html_url").ifBlank { RELEASES_PAGE_URL }
            val notes = json.optString("body").trim()

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }

            if (isNewerVersion(latest = tagName, current = current)) {
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = tagName,
                    releaseUrl = htmlUrl,
                    apkUrl = apkUrl,
                    releaseNotes = notes
                )
            } else {
                UpdateCheckResult.UpToDate(current)
            }
        }.getOrElse { error ->
            UpdateCheckResult.Unavailable(error.localizedMessage ?: "Update check failed")
        }
    }

    internal fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = parseSemver(latest)
        val currentParts = parseSemver(current)
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l != c) return l > c
        }
        return false
    }

    private fun parseSemver(version: String): List<Int> =
        version.trim().removePrefix("v").removePrefix("V")
            .substringBefore('-')
            .split('.')
            .map { it.toIntOrNull() ?: 0 }

    companion object {
        const val LATEST_RELEASE_API = "https://api.github.com/repos/DaDevMikey/GemmA-gent-/releases/latest"
        const val RELEASES_PAGE_URL = "https://github.com/DaDevMikey/GemmA-gent-/releases"
    }
}
