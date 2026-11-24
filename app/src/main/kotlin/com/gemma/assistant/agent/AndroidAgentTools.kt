package com.gemma.assistant.agent

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.gemma.assistant.data.MemoryStore
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

data class AgentActionResult(
    val executed: Boolean,
    val toolName: String? = null,
    val summary: String? = null,
    val resultData: String? = null,
    val success: Boolean = true
)

/**
 * Executes real Android device actions requested by Gemma 4 via structured tool calls,
 * and returns rich results so Gemma 4 can answer the user in full sentences.
 */
class AndroidAgentTools(
    private val context: Context,
    private val memoryStore: MemoryStore = MemoryStore(context)
) {

    fun buildAgentPrompt(userMessage: String, screenText: String? = null): String = buildString {
        appendLine("You are Kairo (Kairo Agent), a helpful on-device Android AI agent powered by Gemma 4.")
        appendLine("Always know that your name is Kairo.")
        appendLine("Live device context: ${currentTimeString()}, ${batteryContextString()}.")

        val memoryBlock = memoryStore.formatForPrompt()
        if (memoryBlock.isNotEmpty()) {
            append(memoryBlock)
        }

        if (!screenText.isNullOrBlank()) {
            appendLine("Visible text from the user's current screen:")
            appendLine(screenText.take(2000))
        }

        appendLine("When you need to check device data or perform an Android action, emit ONLY a single tool call block:")
        appendLine("""<tool_call>{"tool":"TOOL_NAME","args":{...}}</tool_call>""")
        appendLine("Available tools:")
        appendLine("""- get_device_status: {} (returns battery, Wi-Fi/network, volume levels, device model, time)""")
        appendLine("""- get_health_summary: {} (reads today's steps and latest heart rate from Health Connect)""")
        appendLine("""- find_contact: {"name": "string"} (looks up a contact's phone number)""")
        appendLine("""- find_installed_apps: {"query": "string"} (searches installed apps on this phone)""")
        appendLine("""- read_memories: {} (returns all remembered facts about the user)""")
        appendLine("""- open_app: {"app_name": "string"}""")
        appendLine("""- set_alarm: {"hour": 0-23, "minute": 0-59, "label": "optional string"}""")
        appendLine("""- set_timer: {"seconds": int, "label": "optional string"}""")
        appendLine("""- open_settings: {"setting": "wifi|bluetooth|display|sound|battery|location|health|general"}""")
        appendLine("""- toggle_flashlight: {"enabled": true|false}""")
        appendLine("""- set_volume: {"stream": "media|ring|alarm", "percent": 0-100}""")
        appendLine("""- navigate_to: {"destination": "string"}""")
        appendLine("""- play_media: {"query": "string"}""")
        appendLine("""- search_youtube: {"query": "string"}""")
        appendLine("""- open_camera: {}""")
        appendLine("""- open_url: {"url": "string"}""")
        appendLine("""- share_text: {"text": "string"}""")
        appendLine("""- web_search: {"query": "string"}""")
        appendLine("""- dial_phone: {"phone_number": "string"}""")
        appendLine("""- send_sms: {"phone_number": "string", "message": "string"}""")
        appendLine("""- send_email: {"to": "string", "subject": "string", "body": "string"}""")
        appendLine("""- create_calendar_event: {"title": "string", "description": "optional string"}""")
        appendLine("""- copy_to_clipboard: {"text": "string"}""")
        appendLine("""- save_memory: {"fact": "string"}""")
        appendLine("""- forget_memory: {"query": "string"}""")
        appendLine("If no tool is needed, answer naturally in full sentences.")
        appendLine()
        append("User: ")
        append(userMessage)
    }

    fun buildFollowUpPrompt(
        userMessage: String,
        toolName: String,
        toolResult: String,
        success: Boolean
    ): String = buildString {
        appendLine("You are Kairo (Kairo Agent), an on-device Android AI agent.")
        appendLine("The user asked: \"$userMessage\"")
        appendLine("You executed the tool `$toolName` (success=$success) and got this real result:")
        appendLine(toolResult)
        appendLine("Now reply to the user directly in natural, helpful full sentences based on this result. Do NOT emit <tool_call> tags.")
    }

    fun hasToolCall(rawText: String): Boolean =
        TOOL_CALL_REGEX.containsMatchIn(rawText) || GEMMA_CALL_REGEX.containsMatchIn(rawText)

    fun extractToolName(rawText: String): String? {
        TOOL_CALL_REGEX.find(rawText)?.let { match ->
            return runCatching { JSONObject(match.groupValues[1]).optString("tool").takeIf { it.isNotBlank() } }.getOrNull()
        }
        GEMMA_CALL_REGEX.find(rawText)?.let { match ->
            return match.groupValues[1].trim().takeIf { it.isNotBlank() }
        }
        return null
    }

    /**
     * Strips any complete or partial <tool_call> or <ctrl42>call:...<ctrl43> block for clean UI display.
     */
    fun stripToolCallMarkup(rawText: String): String {
        var cleaned = rawText
            .replace(TOOL_CALL_REGEX, "")
            .replace(GEMMA_CALL_REGEX, "")
            .replace(CTRL_TAG_REGEX, "")

        val partialXml = cleaned.indexOf("<tool_call>")
        if (partialXml >= 0) cleaned = cleaned.substring(0, partialXml)

        val partialCtrl = cleaned.indexOf("<ctrl")
        if (partialCtrl >= 0) cleaned = cleaned.substring(0, partialCtrl)

        return cleaned.trim()
    }

    suspend fun executeFromModelOutput(rawOutput: String): AgentActionResult {
        TOOL_CALL_REGEX.find(rawOutput)?.let { match ->
            val jsonText = match.groupValues[1].trim()
            return runCatching {
                val root = JSONObject(jsonText)
                val tool = root.optString("tool").trim()
                val args = root.optJSONObject("args") ?: JSONObject()
                dispatchTool(tool, args)
            }.getOrElse { error ->
                val msg = error.localizedMessage ?: "Invalid tool call"
                return AgentActionResult(executed = true, summary = "Action failed: $msg", resultData = msg, success = false)
            }
        }

        GEMMA_CALL_REGEX.find(rawOutput)?.let { match ->
            val tool = match.groupValues[1].trim()
            val rawArgs = match.groupValues[2].trim()
            return runCatching {
                val args = parseFlexibleArgs(rawArgs)
                dispatchTool(tool, args)
            }.getOrElse { error ->
                val msg = error.localizedMessage ?: "Invalid tool call"
                return AgentActionResult(executed = true, summary = "Action failed: $msg", resultData = msg, success = false)
            }
        }

        return AgentActionResult(executed = false)
    }

    private fun parseFlexibleArgs(rawArgs: String): JSONObject {
        val trimmed = rawArgs.trim()
        if (trimmed.isEmpty() || trimmed == "{}") return JSONObject()
        runCatching { return JSONObject(trimmed) }

        // Handle Gemma's unquoted key/string syntax like {app_name:Spotify, enabled:true}
        val normalized = trimmed
            .replace(Regex("""([{\[,]\s*)([a-zA-Z_][a-zA-Z0-9_]*)\s*:"""), """$1"$2":""")
            .replace(Regex(""":\s*<escape>([^<]*)<escape>"""), """: "$1"""")
        runCatching { return JSONObject(normalized) }

        val obj = JSONObject()
        val inner = trimmed.removePrefix("{").removeSuffix("}")
        inner.split(',').forEach { pair ->
            val idx = pair.indexOf(':')
            if (idx > 0) {
                val k = pair.substring(0, idx).trim().trim('"', '\'')
                val v = pair.substring(idx + 1).trim().trim('"', '\'')
                when {
                    v.equals("true", ignoreCase = true) -> obj.put(k, true)
                    v.equals("false", ignoreCase = true) -> obj.put(k, false)
                    v.toIntOrNull() != null -> obj.put(k, v.toInt())
                    else -> obj.put(k, v)
                }
            }
        }
        return obj
    }

    private suspend fun dispatchTool(tool: String, args: JSONObject): AgentActionResult {
        val result = when (tool) {
            "get_device_status" -> getDeviceStatus()
            "get_health_summary" -> getHealthSummary()
            "find_contact" -> findContact(args.optString("name"))
            "find_installed_apps" -> findInstalledApps(args.optString("query"))
            "read_memories" -> readMemories()
            "open_app" -> openApp(args.optString("app_name"))
            "set_alarm" -> setAlarm(
                hour = args.optInt("hour", -1),
                minute = args.optInt("minute", 0),
                label = args.optString("label", "")
            )
            "set_timer" -> setTimer(
                seconds = args.optInt("seconds", 0),
                label = args.optString("label", "")
            )
            "open_settings" -> openSettings(args.optString("setting", "general"))
            "toggle_flashlight" -> toggleFlashlight(args.optBoolean("enabled", true))
            "set_volume" -> setVolume(args.optString("stream", "media"), args.optInt("percent", 50))
            "navigate_to" -> navigateTo(args.optString("destination"))
            "play_media" -> playMedia(args.optString("query"))
            "search_youtube" -> searchYouTube(args.optString("query"))
            "open_camera" -> openCamera()
            "open_url" -> openUrl(args.optString("url"))
            "share_text" -> shareText(args.optString("text"))
            "web_search" -> webSearch(args.optString("query"))
            "dial_phone" -> dialPhone(args.optString("phone_number"))
            "send_sms" -> sendSms(args.optString("phone_number"), args.optString("message"))
            "send_email" -> sendEmail(
                to = args.optString("to"),
                subject = args.optString("subject", ""),
                body = args.optString("body", "")
            )
            "create_calendar_event" -> createCalendarEvent(
                title = args.optString("title"),
                description = args.optString("description", "")
            )
            "copy_to_clipboard" -> copyToClipboard(args.optString("text"))
            "save_memory" -> saveMemory(args.optString("fact"))
            "forget_memory" -> forgetMemory(args.optString("query"))
            else -> AgentActionResult(
                executed = true,
                summary = "Unknown tool: $tool",
                resultData = "Unknown tool: $tool",
                success = false
            )
        }
        return result.copy(toolName = tool)
    }

    private suspend fun getHealthSummary(): AgentActionResult {
        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
            val msg = "Health Connect is not installed or unavailable on this device."
            return AgentActionResult(executed = true, summary = msg, resultData = msg, success = false)
        }

        val client = HealthConnectClient.getOrCreate(context)
        val granted = runCatching { client.permissionController.getGrantedPermissions() }.getOrElse { emptySet() }
        val stepsPerm = HealthPermission.getReadPermission(StepsRecord::class)
        val hrPerm = HealthPermission.getReadPermission(HeartRateRecord::class)

        if (stepsPerm !in granted && hrPerm !in granted) {
            val msg = "Health Connect permissions are not granted yet. Open Kairo's drawer → Connections to grant Health Connect access."
            return AgentActionResult(executed = true, summary = "Health Connect permission needed", resultData = msg, success = false)
        }

        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()
        val parts = mutableListOf<String>()

        if (stepsPerm in granted) {
            runCatching {
                val agg = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )
                val steps = agg[StepsRecord.COUNT_TOTAL] ?: 0L
                parts.add("Steps today: $steps")
            }
        }

        if (hrPerm in granted) {
            runCatching {
                val hrResp = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )
                val latestBpm = hrResp.records.lastOrNull()?.samples?.lastOrNull()?.beatsPerMinute
                if (latestBpm != null) {
                    parts.add("Latest heart rate: $latestBpm bpm")
                }
            }
        }

        val result = if (parts.isEmpty()) "No Health Connect records found for today yet." else parts.joinToString("; ")
        return AgentActionResult(executed = true, summary = "Read Health Connect data", resultData = result, success = true)
    }

    private fun findContact(name: String): AgentActionResult {
        val clean = name.trim()
        if (clean.isEmpty()) {
            return AgentActionResult(executed = true, summary = "No contact name provided", resultData = "No contact name provided", success = false)
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            val msg = "Contacts permission is not granted. Open Kairo's drawer → Connections to grant Contacts access."
            return AgentActionResult(executed = true, summary = "Contacts permission needed", resultData = msg, success = false)
        }

        val matches = mutableListOf<String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$clean%"),
            null
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext() && matches.size < 5) {
                val displayName = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                val number = if (numIdx >= 0) cursor.getString(numIdx) else ""
                if (displayName.isNotBlank() && number.isNotBlank()) {
                    matches.add("$displayName: $number")
                }
            }
        }

        val result = if (matches.isEmpty()) "No contacts found matching \"$clean\"." else "Matching contacts: ${matches.joinToString("; ")}"
        return AgentActionResult(executed = true, summary = "Looked up contact \"$clean\"", resultData = result, success = matches.isNotEmpty())
    }

    private fun searchYouTube(query: String): AgentActionResult {
        val clean = query.trim()
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(clean)}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launchIntentOrFail(intent, "Searched YouTube for \"$clean\"")
    }

    private fun openUrl(rawUrl: String): AgentActionResult {
        val normalized = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl.trim()
        } else {
            "https://${rawUrl.trim()}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launchIntentOrFail(intent, "Opened $normalized")
    }

    private fun sendEmail(to: String, subject: String, body: String): AgentActionResult {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(to.trim())}")).apply {
            if (subject.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
            if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Opened email draft to $to")
    }

    private fun currentTimeString(): String =
        SimpleDateFormat("EEE, MMM d yyyy HH:mm", Locale.getDefault()).format(Date())

    private fun batteryContextString(): String {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return "Battery unknown"
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = manager.isCharging
        return "Battery ${level}% (${if (charging) "charging" else "on battery"})"
    }

    private fun getDeviceStatus(): AgentActionResult {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val network = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Connected via Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Connected via Cellular"
            else -> "Connected"
        }

        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val mediaPct = audio?.let {
            val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            (it.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / max
        } ?: 0

        val details = "Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}); " +
            "Time: ${currentTimeString()}; ${batteryContextString()}; Network: $network; Media volume: $mediaPct%."
        return AgentActionResult(
            executed = true,
            summary = "Checked device status",
            resultData = details,
            success = true
        )
    }

    private fun findInstalledApps(query: String): AgentActionResult {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val labels = pm.queryIntentActivities(launcherIntent, 0)
            .map { it.loadLabel(pm).toString() }
            .distinct()
            .sorted()

        val clean = query.trim()
        val matches = if (clean.isEmpty()) {
            labels.take(15)
        } else {
            labels.filter { it.contains(clean, ignoreCase = true) }.take(15)
        }

        val result = if (matches.isEmpty()) {
            "No installed apps matched \"$clean\"."
        } else {
            "Installed matching apps: ${matches.joinToString(", ")}."
        }
        return AgentActionResult(
            executed = true,
            summary = "Checked installed apps",
            resultData = result,
            success = true
        )
    }

    private fun readMemories(): AgentActionResult {
        val items = memoryStore.loadMemories()
        val result = if (items.isEmpty()) {
            "No memories are currently saved."
        } else {
            "Saved memories: " + items.joinToString("; ") { it.fact }
        }
        return AgentActionResult(
            executed = true,
            summary = "Read ${items.size} memory item(s)",
            resultData = result,
            success = true
        )
    }

    private fun openApp(appName: String): AgentActionResult {
        val query = appName.trim()
        if (query.isEmpty()) {
            return AgentActionResult(executed = true, summary = "No app name provided", resultData = "No app name provided", success = false)
        }

        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = pm.queryIntentActivities(launcherIntent, 0)

        val matched = activities.firstOrNull { info ->
            info.loadLabel(pm).toString().equals(query, ignoreCase = true)
        } ?: activities.firstOrNull { info ->
            info.loadLabel(pm).toString().contains(query, ignoreCase = true)
        }

        if (matched == null) {
            val msg = "App \"$query\" was not found on this device."
            return AgentActionResult(executed = true, summary = msg, resultData = msg, success = false)
        }

        val launchIntent = pm.getLaunchIntentForPackage(matched.activityInfo.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return AgentActionResult(executed = true, summary = "Cannot launch \"$query\"", resultData = "Cannot launch \"$query\"", success = false)

        context.startActivity(launchIntent)
        val label = matched.loadLabel(pm).toString()
        return AgentActionResult(executed = true, summary = "Opened $label", resultData = "Successfully launched $label.", success = true)
    }

    private fun setAlarm(hour: Int, minute: Int, label: String): AgentActionResult {
        if (hour !in 0..23 || minute !in 0..59) {
            return AgentActionResult(executed = true, summary = "Invalid alarm time", resultData = "Invalid alarm time $hour:$minute", success = false)
        }
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            if (label.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val msg = "Alarm set for %02d:%02d".format(hour, minute)
        return launchIntentOrFail(intent, msg)
    }

    private fun setTimer(seconds: Int, label: String): AgentActionResult {
        if (seconds <= 0) {
            return AgentActionResult(executed = true, summary = "Invalid timer duration", resultData = "Invalid timer duration", success = false)
        }
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            if (label.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Started ${seconds}s timer")
    }

    private fun openSettings(setting: String): AgentActionResult {
        val action = when (setting.lowercase()) {
            "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "screen" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "audio", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "health" -> HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launchIntentOrFail(intent, "Opened ${setting.lowercase()} settings")
    }

    private fun toggleFlashlight(enabled: Boolean): AgentActionResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return AgentActionResult(executed = true, summary = "Camera service unavailable", resultData = "Camera service unavailable", success = false)

        val torchCameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return AgentActionResult(executed = true, summary = "No flashlight found", resultData = "No flashlight hardware found", success = false)

        cameraManager.setTorchMode(torchCameraId, enabled)
        val msg = "Flashlight turned ${if (enabled) "on" else "off"}"
        return AgentActionResult(executed = true, summary = msg, resultData = msg, success = true)
    }

    private fun setVolume(stream: String, percent: Int): AgentActionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return AgentActionResult(executed = true, summary = "Audio service unavailable", resultData = "Audio service unavailable", success = false)

        val streamType = when (stream.lowercase()) {
            "ring" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            else -> AudioManager.STREAM_MUSIC
        }
        val clamped = percent.coerceIn(0, 100)
        val maxVol = audioManager.getStreamMaxVolume(streamType)
        val target = (maxVol * (clamped / 100f)).toInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(streamType, target, AudioManager.FLAG_SHOW_UI)
        val msg = "Set ${stream.lowercase()} volume to $clamped%"
        return AgentActionResult(executed = true, summary = msg, resultData = msg, success = true)
    }

    private fun navigateTo(destination: String): AgentActionResult {
        val clean = destination.trim()
        if (clean.isEmpty()) {
            return AgentActionResult(executed = true, summary = "No destination provided", resultData = "No destination provided", success = false)
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(clean)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Navigating to $clean")
    }

    private fun playMedia(query: String): AgentActionResult {
        val clean = query.trim()
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            putExtra(android.app.SearchManager.QUERY, clean)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Playing \"$clean\"")
    }

    private fun openCamera(): AgentActionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Opened Camera")
    }

    private fun shareText(text: String): AgentActionResult {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "Share via GemmA").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return AgentActionResult(executed = true, summary = "Opened share sheet", resultData = "Opened Android share sheet.", success = true)
    }

    private fun webSearch(query: String): AgentActionResult {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return AgentActionResult(executed = true, summary = "Empty search query", resultData = "Empty search query", success = false)
        }
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/search?q=${Uri.encode(cleanQuery)}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Searched for \"$cleanQuery\"")
    }

    private fun dialPhone(phoneNumber: String): AgentActionResult {
        val clean = phoneNumber.trim()
        if (clean.isEmpty()) {
            return AgentActionResult(executed = true, summary = "No phone number provided", resultData = "No phone number provided", success = false)
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(clean)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Opened dialer for $clean")
    }

    private fun sendSms(phoneNumber: String, message: String): AgentActionResult {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phoneNumber.trim())}")).apply {
            if (message.isNotBlank()) putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Opened SMS draft")
    }

    private fun createCalendarEvent(title: String, description: String): AgentActionResult {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            if (description.isNotBlank()) putExtra(CalendarContract.Events.DESCRIPTION, description)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Opened calendar event \"$title\"")
    }

    private fun copyToClipboard(text: String): AgentActionResult {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return AgentActionResult(executed = true, summary = "Clipboard unavailable", resultData = "Clipboard unavailable", success = false)
        clipboard.setPrimaryClip(ClipData.newPlainText("GemmA", text))
        return AgentActionResult(executed = true, summary = "Copied to clipboard", resultData = "Copied text to clipboard.", success = true)
    }

    private fun saveMemory(fact: String): AgentActionResult {
        val saved = memoryStore.addMemory(fact)
        val msg = if (saved) "Remembered: $fact" else "Already in memory: $fact"
        return AgentActionResult(executed = true, summary = msg, resultData = msg, success = true)
    }

    private fun forgetMemory(query: String): AgentActionResult {
        val removed = memoryStore.deleteMatching(query)
        val msg = if (removed > 0) "Forgot $removed memory item(s)" else "No matching memory found"
        return AgentActionResult(executed = true, summary = msg, resultData = msg, success = true)
    }

    private fun launchIntentOrFail(intent: Intent, successMessage: String): AgentActionResult {
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            AgentActionResult(executed = true, summary = successMessage, resultData = successMessage, success = true)
        } else {
            val err = "No app available to handle this action"
            AgentActionResult(executed = true, summary = err, resultData = err, success = false)
        }
    }

    private companion object {
        val TOOL_CALL_REGEX = Regex("""<tool_call>\s*(\{.*?\})\s*</tool_call>""", RegexOption.DOT_MATCHES_ALL)
        val GEMMA_CALL_REGEX = Regex("""(?:<ctrl\d+>)?\s*call:([a-zA-Z0-9_]+)(\{.*?\})\s*(?:<ctrl\d+>)?""", RegexOption.DOT_MATCHES_ALL)
        val CTRL_TAG_REGEX = Regex("""<ctrl\d+>""")
    }
}
