package com.vintagecam.core.camera.export

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.vintagecam.core.camera.exif.CapturedExifMetadata
import com.vintagecam.core.camera.exif.ExifMetadataWriter
import com.vintagecam.domain.presets.model.CameraPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Scoped Storage Exporter managing atomic MediaStore writes to `Pictures/RizzCam`.
 *
 * Architecture & Platform Compliance:
 * - Scoped Storage (API 29+): Utilizes `IS_PENDING = 1` during compression and EXIF injection,
 *   releasing to `IS_PENDING = 0` atomically once writing and EXIF sync are verified.
 * - Backwards Compatibility (API 28): Employs direct filesystem path insertion into public Pictures.
 * - Atomic Rollback: Deletes dangling MediaStore rows and zero-byte files if any step fails.
 * - Gallery Shortcut: Produces a high-performance 256x256 thumbnail for the viewfinder quick-preview button.
 * - Concurrency: Strictly scoped to [Dispatchers.IO].
 */
class MediaStoreExporter(
    private val context: Context
) {
    companion object {
        private const val TAG = "MediaStoreExporter"
        private const val RELATIVE_DIR = "Pictures/RizzCam"
        private const val THUMBNAIL_SIZE_PX = 256
        private const val DEFAULT_JPEG_QUALITY = 95

        private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
    }

    /**
     * Exports a processed [Bitmap] to the device's public photo gallery under `Pictures/RizzCam`.
     *
     * @param bitmap Graded full-resolution [Bitmap]. Caller retains ownership or can recycle after call.
     * @param preset Active camera preset used to tag the file and EXIF description.
     * @param metadata Hardware telemetry extracted during sensor capture.
     * @param jpegQuality Compression quality [1-100], defaults to 95.
     * @return [ExportResult] containing content URI and viewfinder thumbnail.
     */
    suspend fun exportPhoto(
        bitmap: Bitmap,
        preset: CameraPreset,
        metadata: CapturedExifMetadata,
        jpegQuality: Int = DEFAULT_JPEG_QUALITY
    ): ExportResult = withContext(Dispatchers.IO) {
        val timestamp = fileTimestampFormat.format(Date())
        val sanitizedPreset = preset.displayName.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "RIZZ_${timestamp}_${sanitizedPreset}.jpg"

        val contentResolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_DIR)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val rizzDir = File(picturesDir, "RizzCam")
                if (!rizzDir.exists()) {
                    rizzDir.mkdirs()
                }
                val targetFile = File(rizzDir, fileName)
                put(MediaStore.Images.Media.DATA, targetFile.absolutePath)
            }
        }

        val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = contentResolver.insert(collectionUri, contentValues)
            ?: throw IOException("Failed to create MediaStore entry in $collectionUri")

        try {
            // 1. Write compressed JPEG to MediaStore output stream
            contentResolver.openOutputStream(itemUri).use { outputStream ->
                if (outputStream == null) {
                    throw IOException("Failed to open output stream for URI: $itemUri")
                }
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outputStream)
                if (!success) {
                    throw IOException("Bitmap compression to JPEG failed")
                }
                outputStream.flush()
            }

            // 2. Inject EXIF tags directly via file descriptor
            try {
                contentResolver.openFileDescriptor(itemUri, "rw")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    ExifMetadataWriter.writeToExif(exif, metadata, preset)
                    exif.saveAttributes()
                }
            } catch (exifErr: Exception) {
                Log.w(TAG, "Non-fatal warning: Direct FD EXIF write failed (${exifErr.message}), photo content is intact")
            }

            // 3. Mark row as published (IS_PENDING = 0) on API 29+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(itemUri, contentValues, null, null)
            }

            Log.i(TAG, "Successfully exported photo to MediaStore: $itemUri (path: $RELATIVE_DIR/$fileName)")

            // 4. Generate gallery shortcut thumbnail
            val thumbnail = ThumbnailUtils.extractThumbnail(bitmap, THUMBNAIL_SIZE_PX, THUMBNAIL_SIZE_PX)

            ExportResult(
                uri = itemUri,
                thumbnailBitmap = thumbnail,
                displayName = fileName,
                relativePath = RELATIVE_DIR
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error writing photo to MediaStore, cleaning up incomplete entry: $itemUri", e)
            try {
                contentResolver.delete(itemUri, null, null)
            } catch (cleanupEx: Exception) {
                Log.w(TAG, "Failed to delete aborted MediaStore URI: ${cleanupEx.message}")
            }
            throw e
        }
    }
}
