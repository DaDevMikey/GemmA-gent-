package com.kairo.agent.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseNote(
    val tagName: String,
    val title: String,
    val body: String,
    val publishedDate: String,
    val htmlUrl: String,
    val apkUrl: String?
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseUrl: String,
        val apkUrl: String?,
        val releaseNotes: String
    ) : UpdateCheckResult

    data class UpToDate(
        val currentVersion: String,
        val latestNotes: String = ""
    ) : UpdateCheckResult

    data class Unavailable(val reason: String) : UpdateCheckResult
}

/**
 * Fetches releases and release notes directly from GitHub Releases.
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
            val conn = openGet(LATEST_RELEASE_API, current)
            val code = conn.responseCode
            if (code == 404) {
                return@withContext UpdateCheckResult.Unavailable("No public release found yet (repo may be private).")
            }
            if (code !in 200..299) {
                return@withContext UpdateCheckResult.Unavailable("GitHub returned HTTP $code")
            }

            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val parsed = parseReleaseJson(json)

            if (isNewerVersion(latest = parsed.tagName, current = current)) {
                UpdateCheckResult.UpdateAvailable(
                    currentVersion = current,
                    latestVersion = parsed.tagName,
                    releaseUrl = parsed.htmlUrl,
                    apkUrl = parsed.apkUrl,
                    releaseNotes = parsed.body
                )
            } else {
                UpdateCheckResult.UpToDate(currentVersion = current, latestNotes = parsed.body)
            }
        }.getOrElse { error ->
            UpdateCheckResult.Unavailable(error.localizedMessage ?: "Update check failed")
        }
    }

    suspend fun fetchReleaseNotes(): Result<List<GitHubReleaseNote>> = withContext(Dispatchers.IO) {
        val current = currentVersionName()
        runCatching {
            val conn = openGet(RELEASES_LIST_API, current)
            val code = conn.responseCode
            if (code !in 200..299) {
                error(if (code == 404) "No public releases visible yet (repo may be private)." else "GitHub returned HTTP $code")
            }
            val array = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
            buildList {
                for (i in 0 until array.length()) {
                    add(parseReleaseJson(array.getJSONObject(i)))
                }
            }
        }
    }

    private fun openGet(url: String, currentVersion: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Kairo-Android/$currentVersion")
        }

    private fun parseReleaseJson(json: JSONObject): GitHubReleaseNote {
        val tagName = json.optString("tag_name").trim()
        val title = json.optString("name").ifBlank { tagName }
        val htmlUrl = json.optString("html_url").ifBlank { RELEASES_PAGE_URL }
        val body = json.optString("body").trim()
        val published = json.optString("published_at").take(10)

        var apkUrl: String? = null
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
        }

        return GitHubReleaseNote(
            tagName = tagName,
            title = title,
            body = body,
            publishedDate = published,
            htmlUrl = htmlUrl,
            apkUrl = apkUrl
        )
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
        const val LATEST_RELEASE_API = "https://api.github.com/repos/DaDevMikey/Kairo/releases/latest"
        const val RELEASES_LIST_API = "https://api.github.com/repos/DaDevMikey/Kairo/releases?per_page=8"
        const val RELEASES_PAGE_URL = "https://github.com/DaDevMikey/Kairo/releases"
    }
}
