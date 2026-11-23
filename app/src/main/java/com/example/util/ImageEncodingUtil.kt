package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageEncodingUtil {
    private const val DATA_URI_PREFIX = "data:image/jpeg;base64,"
    private const val MAX_DIMENSION_PX = 180
    private const val MAX_ENCODED_CHARS = 75000

    fun encodePickedImageToDataUri(context: Context, uri: Uri): Result<String> {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val srcWidth = boundsOpts.outWidth
            val srcHeight = boundsOpts.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) {
                return Result.failure(IllegalArgumentException("Selected image could not be decoded."))
            }

            var sampleSize = 1
            val longest = max(srcWidth, srcHeight)
            while (longest / sampleSize > MAX_DIMENSION_PX * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return Result.failure(IllegalArgumentException("Unable to read image stream."))

            val scale = MAX_DIMENSION_PX.toFloat() / max(decoded.width, decoded.height).toFloat()
            val scaled = if (scale < 1f) {
                val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            var quality = 78
            var encoded = ""
            while (quality >= 35) {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val bytes = out.toByteArray()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                encoded = DATA_URI_PREFIX + base64
                if (encoded.length <= MAX_ENCODED_CHARS) break
                quality -= 12
            }
            scaled.recycle()

            if (encoded.length > MAX_ENCODED_CHARS) {
                Result.failure(IllegalStateException("Selected image is too large even after compression."))
            } else {
                Result.success(encoded)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decodeDataUriToImageBitmap(dataUri: String): ImageBitmap? {
        if (!dataUri.startsWith(DATA_URI_PREFIX)) return null
        return try {
            val base64Part = dataUri.removePrefix(DATA_URI_PREFIX)
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
