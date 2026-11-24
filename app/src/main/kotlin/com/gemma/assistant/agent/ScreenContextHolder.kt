package com.gemma.assistant.agent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * Holds the latest screen screenshot and AssistStructure text captured by VoiceInteractionSession,
 * plus helpers to decode user-selected images into bounded Bitmaps for Gemma 4.
 */
object ScreenContextHolder {
    @Volatile
    var capturedScreenshot: Bitmap? = null

    @Volatile
    var extractedScreenText: String? = null

    fun clear() {
        capturedScreenshot = null
        extractedScreenText = null
    }

    fun decodeBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return runCatching {
            val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val maxSide = maxOf(info.size.width, info.size.height)
                    if (maxSide > maxDimension) {
                        val scale = maxDimension.toFloat() / maxSide.toFloat()
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            scaleDownIfNeeded(raw, maxDimension)
        }.getOrNull()
    }

    fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
        val software = if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        val maxSide = maxOf(software.width, software.height)
        if (maxSide <= maxDimension) return software
        val scale = maxDimension.toFloat() / maxSide.toFloat()
        return Bitmap.createScaledBitmap(
            software,
            (software.width * scale).toInt().coerceAtLeast(1),
            (software.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}
