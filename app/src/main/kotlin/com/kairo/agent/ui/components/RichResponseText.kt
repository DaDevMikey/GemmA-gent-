package com.kairo.agent.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import com.kairo.agent.ui.components.kairoCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import com.kairo.agent.ui.components.KairoIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.kairo.agent.ui.components.KairoSurface as Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed interface ResponseSegment {
    data class Prose(val text: String) : ResponseSegment
    data class CodeBlock(val language: String, val code: String) : ResponseSegment
}

/**
 * Renders assistant responses with selectable text, inline `code` & **bold** styling,
 * and dedicated fenced ```code blocks``` with a language header and one-tap Copy button.
 */
@Composable
fun RichResponseText(
    text: String,
    textColor: Color = Color(0xFFE3E3E3),
    modifier: Modifier = Modifier
) {
    val segments = remember(text) { parseSegments(text) }

    SelectionContainer(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            segments.forEach { segment ->
                when (segment) {
                    is ResponseSegment.Prose -> {
                        if (segment.text.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                segment.text.lines().forEach { line ->
                                    val trimmed = line.trimStart()
                                    when {
                                        trimmed.startsWith("### ") -> Text(
                                            text = formatInlineMarkdown(trimmed.removePrefix("### ")),
                                            color = textColor,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        trimmed.startsWith("## ") -> Text(
                                            text = formatInlineMarkdown(trimmed.removePrefix("## ")),
                                            color = Color(0xFFA8C7FA),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                        trimmed.startsWith("# ") -> Text(
                                            text = formatInlineMarkdown(trimmed.removePrefix("# ")),
                                            color = Color(0xFFA8C7FA),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                        trimmed.startsWith("- ") || trimmed.startsWith("* ") -> Text(
                                            text = formatInlineMarkdown("• " + trimmed.drop(2)),
                                            color = textColor,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                        trimmed.isNotEmpty() -> Text(
                                            text = formatInlineMarkdown(line),
                                            color = textColor,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is ResponseSegment.CodeBlock -> {
                        CodeBlockCard(
                            language = segment.language,
                            code = segment.code
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(
    language: String,
    code: String
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2B2D30), kairoCornerShape(12.dp)),
        shape = kairoCornerShape(12.dp),
        color = Color(0xFF141518)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2024))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = language.ifBlank { "code" },
                    color = Color(0xFFA8C7FA),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFF8E918F),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Text(
                text = code,
                color = Color(0xFFE3E3E3),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            )
        }
    }
}

private fun parseSegments(input: String): List<ResponseSegment> {
    val segments = mutableListOf<ResponseSegment>()
    val lines = input.lines()
    val proseBuffer = StringBuilder()
    val codeBuffer = StringBuilder()
    var inCodeBlock = false
    var currentLang = ""

    for (line in lines) {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("```")) {
            if (!inCodeBlock) {
                if (proseBuffer.isNotEmpty()) {
                    segments.add(ResponseSegment.Prose(proseBuffer.toString().trimEnd()))
                    proseBuffer.clear()
                }
                inCodeBlock = true
                currentLang = trimmed.removePrefix("```").trim()
            } else {
                segments.add(
                    ResponseSegment.CodeBlock(
                        language = currentLang,
                        code = codeBuffer.toString().trimEnd()
                    )
                )
                codeBuffer.clear()
                currentLang = ""
                inCodeBlock = false
            }
        } else if (inCodeBlock) {
            if (codeBuffer.isNotEmpty()) codeBuffer.append('\n')
            codeBuffer.append(line)
        } else {
            if (proseBuffer.isNotEmpty()) proseBuffer.append('\n')
            proseBuffer.append(line)
        }
    }

    if (inCodeBlock) {
        segments.add(
            ResponseSegment.CodeBlock(
                language = currentLang,
                code = codeBuffer.toString().trimEnd()
            )
        )
    } else if (proseBuffer.isNotEmpty()) {
        segments.add(ResponseSegment.Prose(proseBuffer.toString().trimEnd()))
    }

    return segments
}

private fun formatInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFF282A2C),
                            color = Color(0xFFA8C7FA)
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
