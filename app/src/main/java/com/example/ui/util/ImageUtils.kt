package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

suspend fun encodeUriToCompactBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val original = BitmapFactory.decodeStream(stream) ?: return@use null
            val maxDim = 480
            val scale = max(original.width, original.height).toFloat() / maxDim
            val scaled = if (scale > 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width / scale).toInt().coerceAtLeast(1),
                    (original.height / scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                original
            }
            val out = ByteArrayOutputStream()
            var quality = 72
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            while (out.size() > 110_000 && quality > 30) {
                out.reset()
                quality -= 12
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            if (scaled !== original) scaled.recycle()
            original.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }
    }.getOrNull()
}

@Composable
fun rememberDecodedBase64Bitmap(base64: String?): ImageBitmap? {
    return remember(base64) {
        if (base64.isNullOrBlank()) return@remember null
        runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
}

fun presetDrawableForKey(key: String?): Int? = when (key) {
    "coffee" -> R.drawable.img_moment_coffee
    "sunset" -> R.drawable.img_moment_sunset
    "trail" -> R.drawable.img_moment_trail
    "studio" -> R.drawable.img_live_studio
    "hero" -> R.drawable.img_bintaface_hero
    else -> null
}
