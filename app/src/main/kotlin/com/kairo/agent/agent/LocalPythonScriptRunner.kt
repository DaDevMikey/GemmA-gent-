package com.kairo.agent.agent

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class PythonScriptResult(
    val output: String,
    val usedNativePython: Boolean,
    val success: Boolean
)

/**
 * Executes local Python calculation/time scripts.
 * Uses native `python3` (e.g. Termux/system) when available and enabled,
 * and always falls back to a sandboxed pure-Kotlin Python-subset evaluator
 * so math, unit, and date/time scripts work on any Android device.
 */
object LocalPythonScriptRunner {

    fun run(
        script: String,
        cacheDir: File? = null,
        allowNativePython: Boolean = true
    ): PythonScriptResult {
        val clean = script.trim()
        if (clean.isEmpty()) {
            return PythonScriptResult("Empty script.", usedNativePython = false, success = false)
        }

        if (allowNativePython && cacheDir != null) {
            findNativePython()?.let { pyBin ->
                val nativeRes = runNativePython(pyBin, clean, cacheDir)
                if (nativeRes != null) return nativeRes
            }
        }

        return runSandboxed(clean)
    }

    private fun findNativePython(): String? {
        val candidates = listOf(
            "/data/data/com.termux/files/usr/bin/python3",
            "/system/bin/python3",
            "/usr/bin/python3",
            "python3"
        )
        return candidates.firstOrNull { path ->
            if (path == "python3") {
                runCatching {
                    val p = ProcessBuilder("which", "python3").start()
                    p.waitFor(1, TimeUnit.SECONDS) && p.exitValue() == 0
                }.getOrDefault(false)
            } else {
                File(path).let { it.exists() && it.canExecute() }
            }
        }
    }

    private fun runNativePython(pyBin: String, script: String, cacheDir: File): PythonScriptResult? {
        return runCatching {
            val file = File(cacheDir, "kairo_script_${System.currentTimeMillis()}.py")
            file.writeText(script)
            try {
                val proc = ProcessBuilder(pyBin, file.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                val finished = proc.waitFor(4, TimeUnit.SECONDS)
                if (!finished) {
                    proc.destroyForcibly()
                    return PythonScriptResult("Python script timed out after 4s.", usedNativePython = true, success = false)
                }
                val out = proc.inputStream.bufferedReader().use { it.readText() }.trim().take(2000)
                val ok = proc.exitValue() == 0
                PythonScriptResult(
                    output = out.ifEmpty { if (ok) "(no output)" else "Script failed (exit ${proc.exitValue()})" },
                    usedNativePython = true,
                    success = ok
                )
            } finally {
                runCatching { file.delete() }
            }
        }.getOrNull()
    }

    internal fun runSandboxed(script: String): PythonScriptResult {
        return runCatching {
            val env = mutableMapOf<String, Any?>(
                "pi" to Math.PI,
                "e" to Math.E
            )
            val prints = mutableListOf<String>()
            var lastValue: Any? = null

            val statements = script
                .lines()
                .map { line ->
                    val hashIdx = findCommentIndex(line)
                    if (hashIdx >= 0) line.substring(0, hashIdx) else line
                }
                .flatMap { splitTopLevelSemicolons(it) }
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            for (stmt in statements) {
                if (stmt.startsWith("import ") || stmt.startsWith("from ")) {
                    continue
                }
                if (stmt.startsWith("print(") && stmt.endsWith(")")) {
                    val inner = stmt.substring(6, stmt.length - 1)
                    val args = splitArgs(inner).map { formatValue(evalExpr(it, env)) }
                    prints.add(args.joinToString(" "))
                    continue
                }
                val assignIdx = findAssignmentIndex(stmt)
                if (assignIdx > 0) {
                    val name = stmt.substring(0, assignIdx).trim()
                    val expr = stmt.substring(assignIdx + 1).trim()
                    val value = evalExpr(expr, env)
                    env[name] = value
                    lastValue = value
                } else {
                    lastValue = evalExpr(stmt, env)
                }
            }

            val output = when {
                prints.isNotEmpty() -> prints.joinToString("\n")
                lastValue != null -> formatValue(lastValue)
                else -> "(done)"
            }
            PythonScriptResult(output = output, usedNativePython = false, success = true)
        }.getOrElse { err ->
            PythonScriptResult(
                output = err.message ?: "Script evaluation error",
                usedNativePython = false,
                success = false
            )
        }
    }

    private fun findCommentIndex(line: String): Int {
        var inSingle = false
        var inDouble = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '\'' && !inDouble) inSingle = !inSingle
            else if (c == '"' && !inSingle) inDouble = !inDouble
            else if (c == '#' && !inSingle && !inDouble) return i
        }
        return -1
    }

    private fun splitTopLevelSemicolons(line: String): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        var inSingle = false
        var inDouble = false
        var start = 0
        for (i in line.indices) {
            val c = line[i]
            if (c == '\'' && !inDouble) inSingle = !inSingle
            else if (c == '"' && !inSingle) inDouble = !inDouble
            else if (!inSingle && !inDouble) {
                when (c) {
                    '(', '[' -> depth++
                    ')', ']' -> depth--
                    ';' -> if (depth == 0) {
                        out.add(line.substring(start, i))
                        start = i + 1
                    }
                }
            }
        }
        out.add(line.substring(start))
        return out
    }

    private fun findAssignmentIndex(stmt: String): Int {
        var depth = 0
        var inSingle = false
        var inDouble = false
        for (i in stmt.indices) {
            val c = stmt[i]
            if (c == '\'' && !inDouble) inSingle = !inSingle
            else if (c == '"' && !inSingle) inDouble = !inDouble
            else if (!inSingle && !inDouble) {
                when (c) {
                    '(', '[' -> depth++
                    ')', ']' -> depth--
                    '=' -> if (depth == 0) {
                        val prev = stmt.getOrNull(i - 1)
                        val next = stmt.getOrNull(i + 1)
                        if (prev !in listOf('=', '!', '<', '>') && next != '=') {
                            val lhs = stmt.substring(0, i).trim()
                            if (lhs.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) return i
                        }
                    }
                }
            }
        }
        return -1
    }

    private fun splitArgs(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        val out = mutableListOf<String>()
        var depth = 0
        var inSingle = false
        var inDouble = false
        var start = 0
        for (i in raw.indices) {
            val c = raw[i]
            if (c == '\'' && !inDouble) inSingle = !inSingle
            else if (c == '"' && !inSingle) inDouble = !inDouble
            else if (!inSingle && !inDouble) {
                when (c) {
                    '(', '[' -> depth++
                    ')', ']' -> depth--
                    ',' -> if (depth == 0) {
                        out.add(raw.substring(start, i).trim())
                        start = i + 1
                    }
                }
            }
        }
        out.add(raw.substring(start).trim())
        return out
    }

    private fun evalExpr(expr: String, env: Map<String, Any?>): Any? {
        return ExprParser(expr.trim(), env).parse()
    }

    private fun formatValue(v: Any?): String = when (v) {
        null -> "None"
        is Double -> if (v % 1.0 == 0.0 && abs(v) < 1e15) v.toLong().toString()
        else "%.8f".format(Locale.US, v).trimEnd('0').trimEnd('.')
        is List<*> -> v.joinToString(", ", prefix = "[", postfix = "]") { formatValue(it) }
        else -> v.toString()
    }

    private class ExprParser(private val s: String, private val env: Map<String, Any?>) {
        private var pos = 0

        fun parse(): Any? {
            val v = parseAddSub()
            skipWs()
            if (pos < s.length) throw IllegalArgumentException("Unexpected token near '${s.substring(pos)}'")
            return v
        }

        private fun skipWs() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        private fun parseAddSub(): Any? {
            var left = parseMulDiv()
            while (true) {
                skipWs()
                if (pos >= s.length) break
                val op = s[pos]
                if (op != '+' && op != '-') break
                pos++
                val right = parseMulDiv()
                left = if (op == '+' && (left is String || right is String)) {
                    formatValue(left) + formatValue(right)
                } else {
                    val a = asDouble(left)
                    val b = asDouble(right)
                    if (op == '+') a + b else a - b
                }
            }
            return left
        }

        private fun parseMulDiv(): Any? {
            var left = parsePower()
            while (true) {
                skipWs()
                if (pos >= s.length) break
                val isFloorDiv = pos + 1 < s.length && s[pos] == '/' && s[pos + 1] == '/'
                val op = s[pos]
                if (!isFloorDiv && op != '*' && op != '/' && op != '%') break
                if (pos + 1 < s.length && s[pos] == '*' && s[pos + 1] == '*') break
                pos += if (isFloorDiv) 2 else 1
                val right = parsePower()
                val a = asDouble(left)
                val b = asDouble(right)
                left = when {
                    isFloorDiv -> floor(a / b)
                    op == '*' -> a * b
                    op == '/' -> {
                        require(b != 0.0) { "Division by zero" }
                        a / b
                    }
                    else -> {
                        require(b != 0.0) { "Modulo by zero" }
                        a % b
                    }
                }
            }
            return left
        }

        private fun parsePower(): Any? {
            val base = parseUnary()
            skipWs()
            if (pos + 1 < s.length && s[pos] == '*' && s[pos + 1] == '*') {
                pos += 2
                val exponent = parsePower()
                return asDouble(base).pow(asDouble(exponent))
            }
            return base
        }

        private fun parseUnary(): Any? {
            skipWs()
            if (pos < s.length && s[pos] == '-') {
                pos++
                return -asDouble(parseUnary())
            }
            if (pos < s.length && s[pos] == '+') {
                pos++
                return asDouble(parseUnary())
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Any? {
            skipWs()
            require(pos < s.length) { "Unexpected end of expression" }
            val c = s[pos]
            if (c == '(') {
                pos++
                val v = parseAddSub()
                skipWs()
                require(pos < s.length && s[pos] == ')') { "Missing closing ')'" }
                pos++
                return v
            }
            if (c == '[') {
                pos++
                val list = mutableListOf<Any?>()
                skipWs()
                if (pos < s.length && s[pos] == ']') {
                    pos++
                    return list
                }
                while (true) {
                    list.add(parseAddSub())
                    skipWs()
                    if (pos < s.length && s[pos] == ',') {
                        pos++
                        continue
                    }
                    require(pos < s.length && s[pos] == ']') { "Missing closing ']'" }
                    pos++
                    return list
                }
            }
            if (c == '"' || c == '\'') {
                val quote = c
                pos++
                val sb = StringBuilder()
                while (pos < s.length && s[pos] != quote) {
                    if (s[pos] == '\\' && pos + 1 < s.length) pos++
                    sb.append(s[pos++])
                }
                require(pos < s.length && s[pos] == quote) { "Unterminated string" }
                pos++
                return sb.toString()
            }
            if (c.isDigit() || c == '.') {
                val start = pos
                while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
                return s.substring(start, pos).toDouble()
            }
            if (c.isLetter() || c == '_') {
                val start = pos
                while (pos < s.length && (s[pos].isLetterOrDigit() || s[pos] == '_' || s[pos] == '.')) pos++
                val ident = s.substring(start, pos)
                skipWs()
                if (pos < s.length && s[pos] == '(') {
                    pos++
                    val args = mutableListOf<Any?>()
                    skipWs()
                    if (pos < s.length && s[pos] == ')') {
                        pos++
                    } else {
                        while (true) {
                            args.add(parseAddSub())
                            skipWs()
                            if (pos < s.length && s[pos] == ',') {
                                pos++
                                continue
                            }
                            require(pos < s.length && s[pos] == ')') { "Missing ')' after $ident(" }
                            pos++
                            break
                        }
                    }
                    return callFunction(ident, args)
                }
                return when (ident) {
                    "math.pi" -> Math.PI
                    "math.e" -> Math.E
                    "True" -> 1.0
                    "False" -> 0.0
                    else -> env[ident] ?: throw IllegalArgumentException("Unknown variable '$ident'")
                }
            }
            throw IllegalArgumentException("Unexpected character '$c'")
        }

        private fun callFunction(name: String, args: List<Any?>): Any? {
            val cleanName = name.removePrefix("math.")
            return when (cleanName) {
                "sqrt" -> sqrt(asDouble(args.first()))
                "abs" -> abs(asDouble(args.first()))
                "floor" -> floor(asDouble(args.first()))
                "ceil" -> ceil(asDouble(args.first()))
                "sin" -> sin(asDouble(args.first()))
                "cos" -> cos(asDouble(args.first()))
                "tan" -> tan(asDouble(args.first()))
                "log" -> ln(asDouble(args.first()))
                "log10" -> log10(asDouble(args.first()))
                "exp" -> exp(asDouble(args.first()))
                "pow" -> asDouble(args[0]).pow(asDouble(args[1]))
                "round" -> {
                    val v = asDouble(args[0])
                    val digits = args.getOrNull(1)?.let { asDouble(it).toInt() } ?: 0
                    if (digits <= 0) round(v) else {
                        val factor = 10.0.pow(digits)
                        round(v * factor) / factor
                    }
                }
                "min" -> flattenNums(args).minOrNull() ?: 0.0
                "max" -> flattenNums(args).maxOrNull() ?: 0.0
                "sum" -> flattenNums(args).sum()
                "len" -> when (val first = args.firstOrNull()) {
                    is List<*> -> first.size.toDouble()
                    is String -> first.length.toDouble()
                    else -> 0.0
                }
                "int" -> asDouble(args.first()).toLong().toDouble()
                "float" -> asDouble(args.first())
                "datetime.now", "datetime.datetime.now", "now" -> {
                    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
                    fmt.format(Date())
                }
                "date.today", "datetime.date.today", "today" -> {
                    SimpleDateFormat("yyyy-MM-dd (EEEE)", Locale.getDefault()).format(Date())
                }
                "time.time", "timestamp" -> (System.currentTimeMillis() / 1000.0)
                "timezone" -> TimeZone.getDefault().id
                else -> throw IllegalArgumentException("Unsupported function '$name'")
            }
        }

        private fun flattenNums(args: List<Any?>): List<Double> {
            return args.flatMap { item ->
                if (item is List<*>) item.map { asDouble(it) } else listOf(asDouble(item))
            }
        }

        private fun asDouble(v: Any?): Double = when (v) {
            is Double -> v
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: throw IllegalArgumentException("Expected number, got '$v'")
            else -> throw IllegalArgumentException("Expected number, got '$v'")
        }
    }
}
