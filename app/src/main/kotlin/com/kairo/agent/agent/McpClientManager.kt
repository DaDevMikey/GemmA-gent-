package com.kairo.agent.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class McpToolInfo(
    val serverUrl: String,
    val name: String,
    val description: String
)

/**
 * Lightweight Model Context Protocol (MCP) JSON-RPC 2.0 over HTTP client.
 * Allows Kairo to discover and invoke tools exposed by user-configured MCP servers.
 */
object McpClientManager {

    fun parseServerUrls(raw: String): List<String> {
        return raw.split('\n', ',')
            .map { it.trim() }
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()
    }

    suspend fun listTools(serverUrl: String): Result<List<McpToolInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = postJsonRpc(serverUrl, "tools/list", JSONObject())
            val toolsArr = resp.optJSONObject("result")?.optJSONArray("tools") ?: JSONArray()
            val out = mutableListOf<McpToolInfo>()
            for (i in 0 until toolsArr.length()) {
                val obj = toolsArr.optJSONObject(i) ?: continue
                val name = obj.optString("name").trim()
                if (name.isNotEmpty()) {
                    out.add(
                        McpToolInfo(
                            serverUrl = serverUrl,
                            name = name,
                            description = obj.optString("description", "").take(120)
                        )
                    )
                }
            }
            out
        }
    }

    suspend fun callTool(
        serverUrl: String,
        toolName: String,
        arguments: JSONObject
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val params = JSONObject()
                .put("name", toolName)
                .put("arguments", arguments)
            val resp = postJsonRpc(serverUrl, "tools/call", params)
            resp.optJSONObject("error")?.let { err ->
                throw IllegalStateException(err.optString("message", "MCP error"))
            }
            val result = resp.optJSONObject("result")
            val contentArr = result?.optJSONArray("content")
            if (contentArr != null && contentArr.length() > 0) {
                val texts = (0 until contentArr.length()).mapNotNull { idx ->
                    contentArr.optJSONObject(idx)?.optString("text")?.takeIf { it.isNotBlank() }
                }
                if (texts.isNotEmpty()) return@runCatching texts.joinToString("\n").take(2500)
            }
            result?.toString()?.take(2500) ?: "MCP tool executed."
        }
    }

    private fun postJsonRpc(serverUrl: String, method: String, params: JSONObject): JSONObject {
        val conn = (URL(serverUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 5000
            readTimeout = 15000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        val req = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", System.currentTimeMillis())
            .put("method", method)
            .put("params", params)

        conn.outputStream.bufferedWriter().use { it.write(req.toString()) }
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            throw IllegalStateException("MCP HTTP $code: ${body.take(200)}")
        }
        return JSONObject(body)
    }
}
