package com.kairo.agent.agent

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
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
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.kairo.agent.data.MemoryStore
import com.kairo.agent.service.NotificationCatchUpStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

enum class AgentPersona(val label: String, val instruction: String) {
    DEFAULT("Default", "Reply naturally, concisely, and helpfully in full sentences."),
    ROAST("Roast", "Be witty, playful, and roast the user lightly while still answering accurately."),
    TLDR("TL;DR", "Respond ONLY with at most 3 ultra-concise bullet points."),
    DEV("Dev Mode", "Output raw code, commands, or technical answers with zero filler."),
    GRAMMAR("Grammar / Reply", "Draft a clean, polite, well-written reply or fix grammar concisely.")
}

data class AgentActionResult(
    val executed: Boolean,
    val toolName: String? = null,
    val summary: String? = null,
    val resultData: String? = null,
    val success: Boolean = true
)

/**
 * Executes real Android device actions requested by Gemma 4 via structured tool calls,
 * manages multi-turn conversation context + automatic memory extraction,
 * and supports calling, texting, sending images, Health Connect, notifications, and optional root shell.
 */
class AndroidAgentTools(
    private val context: Context,
    private val memoryStore: MemoryStore = MemoryStore(context)
) {

    fun buildAgentPrompt(
        userMessage: String,
        screenText: String? = null,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        persona: AgentPersona = AgentPersona.DEFAULT,
        rootPowerMode: Boolean = false
    ): String = buildString {
        appendLine("You are Kairo (Kairo Agent), a helpful on-device Android AI agent powered by Gemma 4.")
        appendLine("Persona instruction: ${persona.instruction}")
        appendLine("Live device context: ${currentTimeString()}, ${batteryContextString()}.")

        if (persona == AgentPersona.GRAMMAR) {
            val clip = readClipboardText()
            if (clip.isNotBlank()) {
                appendLine("User's current clipboard text: \"$clip\"")
            }
        }

        val memoryBlock = memoryStore.formatForPrompt()
        if (memoryBlock.isNotEmpty()) {
            append(memoryBlock)
        }

        appendLine("Memory rule: ONLY emit <memory>fact about the user</memory> when the USER explicitly shares personal facts, preferences, their name, routines, or goals about THEMSELVES. NEVER save your own responses, device status, tool outputs, or general facts.")

        if (!screenText.isNullOrBlank()) {
            appendLine("Visible text from the user's current screen:")
            appendLine(screenText.take(2000))
        }

        if (conversationHistory.isNotEmpty()) {
            appendLine("Recent conversation context:")
            conversationHistory.takeLast(6).forEach { (prevUser, prevAssistant) ->
                appendLine("User: $prevUser")
                if (prevAssistant.isNotBlank()) {
                    appendLine("Kairo: ${prevAssistant.take(400)}")
                }
            }
        }

        appendLine("When you need to check device data or perform an Android action, emit ONLY a single tool call block:")
        appendLine("""<tool_call>{"tool":"TOOL_NAME","args":{...}}</tool_call>""")
        appendLine("Available tools:")
        appendLine("""- get_device_status: {}""")
        appendLine("""- get_current_location: {} (reads current GPS/network location & city/address locally)""")
        appendLine("""- get_notifications: {} (reads recent unread notifications for catch-up)""")
        appendLine("""- call_person: {"target": "contact name or phone number"}""")
        appendLine("""- text_person: {"target": "contact name or phone number", "message": "string"}""")
        appendLine("""- send_image_to_person: {"caption": "optional string"} (sends the attached image to a person/app)""")
        appendLine("""- find_contact: {"name": "string"}""")
        appendLine("""- find_installed_apps: {"query": "string"}""")
        appendLine("""- read_memories: {}""")
        appendLine("""- open_app: {"app_name": "string"}""")
        appendLine("""- open_split_screen: {"app_name": "string", "second_app_name": "optional string"} (launches app(s) side-by-side in split screen)""")
        appendLine("""- media_control: {"action": "play_pause|next|previous|stop"}""")
        appendLine("""- open_spotify: {"query": "optional song/artist/playlist"}""")
        appendLine("""- send_whatsapp: {"target": "contact name or phone", "message": "string"}""")
        appendLine("""- open_maps_search: {"query": "string"}""")
        appendLine("""- search_play_store: {"query": "string"}""")
        appendLine("""- set_alarm: {"hour": 0-23, "minute": 0-59, "label": "optional string"}""")
        appendLine("""- set_timer: {"seconds": int, "label": "optional string"}""")
        appendLine("""- open_settings: {"setting": "wifi|bluetooth|display|sound|battery|location|health|notifications|general"}""")
        appendLine("""- toggle_flashlight: {"enabled": true|false}""")
        appendLine("""- set_volume: {"stream": "media|ring|alarm", "percent": 0-100}""")
        appendLine("""- set_brightness: {"percent": 0-100}""")
        appendLine("""- navigate_to: {"destination": "string"}""")
        appendLine("""- play_media: {"query": "string"}""")
        appendLine("""- search_youtube: {"query": "string"}""")
        appendLine("""- open_camera: {}""")
        appendLine("""- open_url: {"url": "string"}""")
        appendLine("""- share_text: {"text": "string"}""")
        appendLine("""- web_search: {"query": "string"}""")
        appendLine("""- send_email: {"to": "string", "subject": "string", "body": "string"}""")
        appendLine("""- create_calendar_event: {"title": "string", "description": "optional string"}""")
        appendLine("""- get_weather: {"location": "string"}""")
        appendLine("""- open_clock: {} (opens alarms, timers, and stopwatch)""")
        appendLine("""- create_note: {"title": "string", "content": "string"} (saves or shares a quick note)""")
        appendLine("""- copy_to_clipboard: {"text": "string"}""")
        appendLine("""- save_memory: {"fact": "string"}""")
        appendLine("""- forget_memory: {"query": "string"}""")
        appendLine()
        append("User: ")
        append(userMessage)
    }

    fun buildFollowUpPrompt(
        userMessage: String,
        toolName: String,
        toolResult: String,
        success: Boolean,
        persona: AgentPersona = AgentPersona.DEFAULT
    ): String = buildString {
        appendLine("You are Kairo (Kairo Agent), an on-device Android AI agent.")
        appendLine("Persona instruction: ${persona.instruction}")
        appendLine("The user asked: \"$userMessage\"")
        appendLine("You executed `$toolName` (success=$success) and got this real result:")
        appendLine(toolResult)
        appendLine("Now reply to the user directly in natural full sentences based on this result. Do NOT emit <tool_call> tags.")
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

    fun extractAndSaveMemories(rawText: String): List<String> {
        val saved = mutableListOf<String>()
        MEMORY_TAG_REGEX.findAll(rawText).forEach { match ->
            val fact = match.groupValues[1].trim()
            val lower = fact.lowercase()
            val looksLikeModelOrDeviceOutput = lower.startsWith("i am ") ||
                lower.startsWith("your battery") ||
                lower.contains("android ") ||
                lower.contains("sm-") ||
                lower.contains("tok/s")
            if (fact.isNotEmpty() && !looksLikeModelOrDeviceOutput && memoryStore.addMemory(fact)) {
                saved.add(fact)
            }
        }
        return saved
    }

    fun stripToolCallMarkup(rawText: String): String {
        var cleaned = rawText
            .replace(TOOL_CALL_REGEX, "")
            .replace(GEMMA_CALL_REGEX, "")
            .replace(MEMORY_TAG_REGEX, "")
            .replace(CTRL_TAG_REGEX, "")

        val partialXml = cleaned.indexOf("<tool_call>")
        if (partialXml >= 0) cleaned = cleaned.substring(0, partialXml)

        val partialMem = cleaned.indexOf("<memory>")
        if (partialMem >= 0) cleaned = cleaned.substring(0, partialMem)

        val partialCtrl = cleaned.indexOf("<ctrl")
        if (partialCtrl >= 0) cleaned = cleaned.substring(0, partialCtrl)

        return cleaned.trim()
    }

    suspend fun executeFromModelOutput(
        rawOutput: String,
        attachedImage: Bitmap? = null,
        rootPowerMode: Boolean = false
    ): AgentActionResult {
        TOOL_CALL_REGEX.find(rawOutput)?.let { match ->
            val jsonText = match.groupValues[1].trim()
            return runCatching {
                val root = JSONObject(jsonText)
                val tool = root.optString("tool").trim()
                val args = root.optJSONObject("args") ?: JSONObject()
                dispatchTool(tool, args, attachedImage, rootPowerMode)
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
                dispatchTool(tool, args, attachedImage, rootPowerMode)
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

    private suspend fun dispatchTool(
        tool: String,
        args: JSONObject,
        attachedImage: Bitmap?,
        rootPowerMode: Boolean
    ): AgentActionResult {
        val result = when (tool) {
            "get_device_status" -> getDeviceStatus()
            "get_current_location" -> getCurrentLocation()
            "get_notifications" -> getNotifications()
            "call_person" -> callPerson(args.optString("target").ifBlank { args.optString("phone_number") })
            "text_person" -> textPerson(
                target = args.optString("target").ifBlank { args.optString("phone_number") },
                message = args.optString("message", "")
            )
            "send_image_to_person" -> sendImageToPerson(attachedImage, args.optString("caption", ""))
            "find_contact" -> findContact(args.optString("name"))
            "find_installed_apps" -> findInstalledApps(args.optString("query"))
            "read_memories" -> readMemories()
            "open_app" -> openApp(args.optString("app_name"))
            "open_split_screen" -> openSplitScreen(args.optString("app_name"), args.optString("second_app_name", ""))
            "media_control" -> mediaControl(args.optString("action", "play_pause"))
            "open_spotify" -> openSpotify(args.optString("query", ""))
            "send_whatsapp" -> sendWhatsApp(args.optString("target"), args.optString("message", ""))
            "open_maps_search" -> openMapsSearch(args.optString("query"))
            "search_play_store" -> searchPlayStore(args.optString("query"))
            "set_alarm" -> setAlarm(args.optInt("hour", -1), args.optInt("minute", 0), args.optString("label", ""))
            "set_timer" -> setTimer(args.optInt("seconds", 0), args.optString("label", ""))
            "open_settings" -> openSettings(args.optString("setting", "general"))
            "toggle_flashlight" -> toggleFlashlight(args.optBoolean("enabled", true))
            "set_volume" -> setVolume(args.optString("stream", "media"), args.optInt("percent", 50))
            "set_brightness" -> setBrightness(args.optInt("percent", 50))
            "navigate_to" -> navigateTo(args.optString("destination"))
            "play_media" -> playMedia(args.optString("query"))
            "search_youtube" -> searchYouTube(args.optString("query"))
            "open_camera" -> openCamera()
            "open_url" -> openUrl(args.optString("url"))
            "share_text" -> shareText(args.optString("text"))
            "web_search" -> webSearch(args.optString("query"))
            "dial_phone" -> callPerson(args.optString("phone_number"))
            "send_sms" -> textPerson(args.optString("phone_number"), args.optString("message"))
            "send_email" -> sendEmail(args.optString("to"), args.optString("subject", ""), args.optString("body", ""))
            "create_calendar_event" -> createCalendarEvent(args.optString("title"), args.optString("description", ""))
            "get_weather" -> webSearch("weather ${args.optString("location")}".trim())
            "open_clock" -> launchIntentOrFail(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "Opened Clock & Alarms")
            "create_note" -> shareText("${args.optString("title")}\n\n${args.optString("content")}".trim())
            "copy_to_clipboard" -> copyToClipboard(args.optString("text"))
            "save_memory" -> saveMemory(args.optString("fact"))
            "forget_memory" -> forgetMemory(args.optString("query"))
            else -> AgentActionResult(executed = true, summary = "Unknown tool: $tool", resultData = "Unknown tool: $tool", success = false)
        }
        return result.copy(toolName = tool)
    }

    private fun readClipboardText(): String {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return ""
        return cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty().take(600)
    }

    private fun getNotifications(): AgentActionResult {
        if (!NotificationCatchUpStore.isAccessGranted(context)) {
            val msg = "Notification Catch-Up access is not enabled yet. Open Kairo's drawer → Connections to enable Notification Access."
            return AgentActionResult(executed = true, summary = "Notification access needed", resultData = msg, success = false)
        }
        val summary = NotificationCatchUpStore.formatSummary()
        return AgentActionResult(executed = true, summary = "Read recent notifications", resultData = summary, success = true)
    }

    private fun resolveContactNumber(nameOrNumber: String): Pair<String, String>? {
        val clean = nameOrNumber.trim()
        if (clean.isEmpty()) return null
        if (clean.any { it.isDigit() } && clean.count { it.isDigit() } >= 3) {
            return clean to clean
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null

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
            if (cursor.moveToFirst()) {
                val nIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val pIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val display = if (nIdx >= 0) cursor.getString(nIdx) else clean
                val number = if (pIdx >= 0) cursor.getString(pIdx) else ""
                if (number.isNotBlank()) return display to number
            }
        }
        return null
    }

    private fun callPerson(target: String): AgentActionResult {
        val resolved = resolveContactNumber(target)
            ?: return AgentActionResult(
                executed = true,
                summary = "Could not find phone number for \"$target\"",
                resultData = "No phone number found for \"$target\" (grant Contacts permission or provide a phone number).",
                success = false
            )
        val (display, number) = resolved
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launchIntentOrFail(intent, "Opened dialer for $display ($number)")
    }

    private fun textPerson(target: String, message: String): AgentActionResult {
        val resolved = resolveContactNumber(target)
            ?: return AgentActionResult(
                executed = true,
                summary = "Could not find number for \"$target\"",
                resultData = "No phone number found for \"$target\" (grant Contacts permission or provide a phone number).",
                success = false
            )
        val (display, number) = resolved
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).apply {
            if (message.isNotBlank()) putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Opened SMS to $display")
    }

    private fun sendImageToPerson(attachedImage: Bitmap?, caption: String): AgentActionResult {
        if (attachedImage == null) {
            return AgentActionResult(
                executed = true,
                summary = "No image attached to send",
                resultData = "No image is currently attached. Attach an image first, then ask Kairo to send it.",
                success = false
            )
        }
        return runCatching {
            val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(dir, "kairo_share_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                attachedImage.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            val uri = FileProvider.getUriForFile(context, "com.kairo.agent.fileprovider", file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (caption.isNotBlank()) putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Send image via Kairo").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            AgentActionResult(executed = true, summary = "Opened share sheet for image", resultData = "Opened Android share sheet with the attached image.", success = true)
        }.getOrElse { e ->
            AgentActionResult(executed = true, summary = "Failed to share image", resultData = e.localizedMessage ?: "Share failed", success = false)
        }
    }

    private fun setBrightness(percent: Int): AgentActionResult {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launchIntentOrFail(intent, "Opened Display Settings to adjust brightness ($percent%)")
    }

    private suspend fun runShellCommand(command: String, rootPowerMode: Boolean): AgentActionResult = withContext(Dispatchers.IO) {
        if (!rootPowerMode) {
            return@withContext AgentActionResult(
                executed = true,
                summary = "Power Mode is disabled",
                resultData = "Enable Root / Shell Power Mode in Kairo's Setup & Compatibility sheet to run shell commands.",
                success = false
            )
        }
        val clean = command.trim()
        if (clean.isEmpty()) {
            return@withContext AgentActionResult(executed = true, summary = "Empty shell command", resultData = "No command provided.", success = false)
        }
        runCatching {
            val useSu = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su").any { File(it).exists() }
            val cmdArray = if (useSu) arrayOf("su", "-c", clean) else arrayOf("sh", "-c", clean)
            val proc = ProcessBuilder(*cmdArray).redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().use { it.readText() }.take(2000)
            val exit = proc.waitFor()
            AgentActionResult(
                executed = true,
                summary = "Ran shell command (exit $exit)",
                resultData = "Exit $exit:\n$output",
                success = exit == 0
            )
        }.getOrElse { e ->
            AgentActionResult(executed = true, summary = "Shell error", resultData = e.localizedMessage ?: "Failed", success = false)
        }
    }

    @Suppress("DEPRECATION")
    private fun getCurrentLocation(): AgentActionResult {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            return AgentActionResult(
                executed = true,
                summary = "Location permission required",
                resultData = "Location permission is not granted yet. Ask the user to allow Location in Kairo's Connections & Permissions.",
                success = false
            )
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return AgentActionResult(executed = true, summary = "Location service unavailable", resultData = "LocationManager unavailable", success = false)

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var best: Location? = null
        for (p in providers) {
            val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull() ?: continue
            if (best == null || loc.time > best.time) best = loc
        }
        if (best == null) {
            return AgentActionResult(
                executed = true,
                summary = "Location unavailable",
                resultData = "No recent location fix found. Make sure device Location/GPS is turned on.",
                success = false
            )
        }
        val addressLine = runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val list = geocoder.getFromLocation(best.latitude, best.longitude, 1)
            val addr = list?.firstOrNull()
            listOfNotNull(addr?.thoroughfare, addr?.locality, addr?.adminArea, addr?.countryName)
                .distinct()
                .joinToString(", ")
                .takeIf { it.isNotBlank() }
        }.getOrNull()

        val coords = "%.5f, %.5f".format(Locale.US, best.latitude, best.longitude)
        val result = if (addressLine != null) "$addressLine ($coords)" else coords
        return AgentActionResult(
            executed = true,
            summary = addressLine ?: "Located ($coords)",
            resultData = "Current device location: $result",
            success = true
        )
    }

    private fun findContact(name: String): AgentActionResult {
        val clean = name.trim()
        if (clean.isEmpty()) return AgentActionResult(executed = true, summary = "No contact name provided", resultData = "No contact name provided", success = false)
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            val msg = "Contacts permission is not granted. Open Kairo's drawer → Connections to grant Contacts access."
            return AgentActionResult(executed = true, summary = "Contacts permission needed", resultData = msg, success = false)
        }
        val matches = mutableListOf<String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$clean%"),
            null
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext() && matches.size < 5) {
                val displayName = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                val number = if (numIdx >= 0) cursor.getString(numIdx) else ""
                if (displayName.isNotBlank() && number.isNotBlank()) matches.add("$displayName: $number")
            }
        }
        val result = if (matches.isEmpty()) "No contacts found matching \"$clean\"." else "Matching contacts: ${matches.joinToString("; ")}"
        return AgentActionResult(executed = true, summary = "Looked up contact \"$clean\"", resultData = result, success = matches.isNotEmpty())
    }

    private fun searchYouTube(query: String): AgentActionResult {
        val clean = query.trim()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(clean)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launchIntentOrFail(intent, "Searched YouTube for \"$clean\"")
    }

    private fun openUrl(rawUrl: String): AgentActionResult {
        val normalized = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl.trim() else "https://${rawUrl.trim()}"
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

    private fun currentTimeString(): String = SimpleDateFormat("EEE, MMM d yyyy HH:mm", Locale.getDefault()).format(Date())

    private fun batteryContextString(): String {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return "Battery unknown"
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "Battery $level% (${if (manager.isCharging) "charging" else "on battery"})"
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
        val details = "Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}); Time: ${currentTimeString()}; ${batteryContextString()}; Network: $network; Media volume: $mediaPct%."
        return AgentActionResult(executed = true, summary = "Checked device status", resultData = details, success = true)
    }

    private fun findInstalledApps(query: String): AgentActionResult {
        val pm = context.packageManager
        val labels = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.loadLabel(pm).toString() }.distinct().sorted()
        val clean = query.trim()
        val matches = if (clean.isEmpty()) labels.take(15) else labels.filter { it.contains(clean, ignoreCase = true) }.take(15)
        val result = if (matches.isEmpty()) "No installed apps matched \"$clean\"." else "Installed matching apps: ${matches.joinToString(", ")}."
        return AgentActionResult(executed = true, summary = "Checked installed apps", resultData = result, success = true)
    }

    private fun readMemories(): AgentActionResult {
        val items = memoryStore.loadMemories()
        val result = if (items.isEmpty()) "No memories are currently saved." else "Saved memories: " + items.joinToString("; ") { it.fact }
        return AgentActionResult(executed = true, summary = "Read ${items.size} memory item(s)", resultData = result, success = true)
    }

    private fun resolveAppLaunchIntent(appName: String): Pair<String, Intent>? {
        val query = appName.trim()
        if (query.isEmpty()) return null
        val pm = context.packageManager
        val activities = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        val matched = activities.firstOrNull { it.loadLabel(pm).toString().equals(query, ignoreCase = true) }
            ?: activities.firstOrNull { it.loadLabel(pm).toString().contains(query, ignoreCase = true) }
            ?: return null
        val launchIntent = pm.getLaunchIntentForPackage(matched.activityInfo.packageName) ?: return null
        return matched.loadLabel(pm).toString() to launchIntent
    }

    private fun openApp(appName: String): AgentActionResult {
        val query = appName.trim()
        if (query.isEmpty()) return AgentActionResult(executed = true, summary = "No app name provided", resultData = "No app name provided", success = false)
        val (label, launchIntent) = resolveAppLaunchIntent(query)
            ?: return AgentActionResult(executed = true, summary = "App \"$query\" not found", resultData = "App \"$query\" was not found.", success = false)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return AgentActionResult(executed = true, summary = "Opened $label", resultData = "Successfully launched $label.", success = true)
    }

    private fun openSplitScreen(appName: String, secondAppName: String): AgentActionResult {
        val first = resolveAppLaunchIntent(appName)
            ?: return AgentActionResult(executed = true, summary = "App \"$appName\" not found", resultData = "Could not find app \"$appName\".", success = false)
        val second = if (secondAppName.isNotBlank()) resolveAppLaunchIntent(secondAppName) else null

        return runCatching {
            if (second != null) {
                first.second.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(first.second)
                second.second.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
                context.startActivity(second.second)
                AgentActionResult(
                    executed = true,
                    summary = "Opened ${first.first} + ${second.first} in split screen",
                    resultData = "Launched ${first.first} alongside ${second.first} using Android multi-window split flags.",
                    success = true
                )
            } else {
                first.second.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
                context.startActivity(first.second)
                AgentActionResult(
                    executed = true,
                    summary = "Opened ${first.first} in split screen",
                    resultData = "Launched ${first.first} in an adjacent split-screen window.",
                    success = true
                )
            }
        }.getOrElse { err ->
            AgentActionResult(executed = true, summary = "Split screen failed", resultData = err.localizedMessage ?: "Unable to launch split screen.", success = false)
        }
    }

    private fun mediaControl(action: String): AgentActionResult {
        val keyCode = when (action.lowercase(Locale.ROOT).trim()) {
            "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev", "back" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return AgentActionResult(executed = true, summary = "Audio service unavailable", resultData = "AudioManager unavailable", success = false)
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return AgentActionResult(executed = true, summary = "Media action: $action", resultData = "Dispatched media key ($action).", success = true)
    }

    private fun openSpotify(query: String): AgentActionResult {
        val clean = query.trim()
        val intent = if (clean.isNotEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:${Uri.encode(clean)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            context.packageManager.getLaunchIntentForPackage("com.spotify.music")?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return launchIntentOrFail(intent, if (clean.isNotEmpty()) "Opened Spotify for \"$clean\"" else "Opened Spotify")
    }

    private fun sendWhatsApp(target: String, message: String): AgentActionResult {
        val resolved = resolveContactNumber(target)
        val digits = (resolved?.second ?: target).filter { it.isDigit() }
        val encodedMsg = Uri.encode(message)
        val url = if (digits.isNotEmpty()) "https://wa.me/$digits?text=$encodedMsg" else "https://wa.me/?text=$encodedMsg"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val label = resolved?.first ?: target.ifBlank { "WhatsApp" }
        return launchIntentOrFail(intent, "Prepared WhatsApp message to $label")
    }

    private fun openMapsSearch(query: String): AgentActionResult {
        val clean = query.trim()
        if (clean.isEmpty()) return AgentActionResult(executed = true, summary = "No query provided", resultData = "No query provided", success = false)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(clean)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Searched Maps for \"$clean\"")
    }

    private fun searchPlayStore(query: String): AgentActionResult {
        val clean = query.trim()
        if (clean.isEmpty()) return AgentActionResult(executed = true, summary = "No query provided", resultData = "No query provided", success = false)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(clean)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Searched Play Store for \"$clean\"")
    }

    private fun setAlarm(hour: Int, minute: Int, label: String): AgentActionResult {
        if (hour !in 0..23 || minute !in 0..59) return AgentActionResult(executed = true, summary = "Invalid alarm time", resultData = "Invalid alarm time", success = false)
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            if (label.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentOrFail(intent, "Alarm set for %02d:%02d".format(hour, minute))
    }

    private fun setTimer(seconds: Int, label: String): AgentActionResult {
        if (seconds <= 0) return AgentActionResult(executed = true, summary = "Invalid timer duration", resultData = "Invalid timer duration", success = false)
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
            "notifications" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return launchIntentOrFail(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "Opened ${setting.lowercase()} settings")
    }

    private fun toggleFlashlight(enabled: Boolean): AgentActionResult {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return AgentActionResult(executed = true, summary = "Camera service unavailable", resultData = "Camera service unavailable", success = false)
        val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
            ?: return AgentActionResult(executed = true, summary = "No flashlight found", resultData = "No flashlight found", success = false)
        cm.setTorchMode(id, enabled)
        val msg = "Flashlight turned ${if (enabled) "on" else "off"}"
        return AgentActionResult(executed = true, summary = msg, resultData = msg, success = true)
    }

    private fun setVolume(stream: String, percent: Int): AgentActionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return AgentActionResult(executed = true, summary = "Audio service unavailable", resultData = "Audio service unavailable", success = false)
        val streamType = when (stream.lowercase()) {
            "ring" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            else -> AudioManager.STREAM_MUSIC
        }
        val clamped = percent.coerceIn(0, 100)
        val maxVol = am.getStreamMaxVolume(streamType)
        am.setStreamVolume(streamType, (maxVol * (clamped / 100f)).toInt().coerceIn(0, maxVol), AudioManager.FLAG_SHOW_UI)
        val msg = "Set ${stream.lowercase()} volume to $clamped%"
        return AgentActionResult(executed = true, summary = msg, resultData = msg, success = true)
    }

    private fun navigateTo(destination: String): AgentActionResult {
        val clean = destination.trim()
        if (clean.isEmpty()) return AgentActionResult(executed = true, summary = "No destination provided", resultData = "No destination provided", success = false)
        return launchIntentOrFail(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(clean)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "Navigating to $clean")
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

    private fun openCamera(): AgentActionResult =
        launchIntentOrFail(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "Opened Camera")

    private fun shareText(text: String): AgentActionResult {
        val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share via Kairo").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return AgentActionResult(executed = true, summary = "Opened share sheet", resultData = "Opened share sheet.", success = true)
    }

    private fun webSearch(query: String): AgentActionResult {
        val clean = query.trim()
        if (clean.isEmpty()) return AgentActionResult(executed = true, summary = "Empty search query", resultData = "Empty search query", success = false)
        return launchIntentOrFail(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(clean)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "Searched for \"$clean\"")
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
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return AgentActionResult(executed = true, summary = "Clipboard unavailable", resultData = "Clipboard unavailable", success = false)
        cm.setPrimaryClip(ClipData.newPlainText("Kairo", text))
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
            AgentActionResult(executed = true, summary = "No app available to handle this action", resultData = "No app available", success = false)
        }
    }

    private companion object {
        val TOOL_CALL_REGEX = Regex("""<tool_call>\s*(\{.*?\})\s*</tool_call>""", RegexOption.DOT_MATCHES_ALL)
        val GEMMA_CALL_REGEX = Regex("""(?:<ctrl\d+>)?\s*call:([a-zA-Z0-9_]+)(\{.*?\})\s*(?:<ctrl\d+>)?""", RegexOption.DOT_MATCHES_ALL)
        val MEMORY_TAG_REGEX = Regex("""<memory>(.*?)</memory>""", RegexOption.DOT_MATCHES_ALL)
        val CTRL_TAG_REGEX = Regex("""<ctrl\d+>""")
    }
}
