package de.healthforge.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import de.healthforge.BuildConfig
import de.healthforge.data.network.MediaApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps [MediaApi] with client-side image compression (REQ-RECIPE-006).
 * Decode → orient → downscale to max 1080px on the longer edge → JPEG quality 85.
 */
@Singleton
class MediaRepository @Inject constructor(
    private val api: MediaApi,
) {

    suspend fun uploadImage(ctx: Context, bucket: String, uri: Uri): Result<String> = runCatching {
        val bytes = compress(ctx, uri)
        val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "image.jpg", body)
        api.upload(bucket, part).key
    }

    private fun compress(ctx: Context, uri: Uri): ByteArray {
        val cr = ctx.contentResolver

        // Read ALL bytes into memory first — avoids ContentResolver stream issues
        // with FileProvider URIs (camera photos) where multiple openInputStream
        // calls may fail on some Android versions.
        val allBytes = cr.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Cannot open input stream")

        // Step 1: decode bounds with inJustDecodeBounds
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(allBytes, 0, allBytes.size, bounds)
        val (w, h) = bounds.outWidth to bounds.outHeight
        val longEdge = maxOf(w, h)
        val maxSize = 1080
        var sample = 1
        while ((longEdge / sample) > maxSize * 2) sample *= 2

        // Step 2: decode with sampling
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = BitmapFactory.decodeByteArray(allBytes, 0, allBytes.size, opts)
            ?: error("Cannot decode bitmap")

        // Step 3: respect EXIF orientation (use the byte array directly)
        val oriented = applyExifRotation(allBytes, raw)

        // Step 4: scale to maxSize on longer edge
        val sw = oriented.width
        val sh = oriented.height
        val ratio = maxSize.toFloat() / maxOf(sw, sh)
        val scaled = if (ratio < 1f) {
            Bitmap.createScaledBitmap(oriented, (sw * ratio).toInt(), (sh * ratio).toInt(), true)
        } else oriented

        // Step 5: encode to JPEG Q85
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        if (scaled !== raw && scaled !== oriented) scaled.recycle()
        if (oriented !== raw) oriented.recycle()
        raw.recycle()
        return out.toByteArray()
    }

    /** Reads EXIF orientation from the byte array directly (no ContentResolver needed). */
    private fun applyExifRotation(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ByteArrayInputStream(bytes).use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val deg = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (deg == 0f) return bitmap
        val m = Matrix().apply { postRotate(deg) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    companion object {
        /** Public CDN URL for a given bucket + base key + variant (`thumb`, `medium`, `full`). */
        fun imageUrl(bucket: String, baseKey: String?, variant: String = "medium"): String? {
            if (baseKey.isNullOrBlank()) return null
            // If the key is a full URL (http/https), return it directly (e.g. seed placeholder images)
            if (baseKey.startsWith("http://") || baseKey.startsWith("https://")) return baseKey
            return "${BuildConfig.MEDIA_BASE_URL}$bucket/${baseKey}__${variant}.jpg"
        }
    }
}
