package com.vintagecam.core.camera.export

import android.graphics.Bitmap
import android.net.Uri

/**
 * Result model returned when a photo is exported to MediaStore storage.
 *
 * @param uri Content [Uri] of the inserted image row in MediaStore.
 * @param thumbnailBitmap Fast-loading scaled thumbnail for the viewfinder gallery shortcut button.
 * @param displayName File name assigned to the image file (e.g., "RIZZ_20261022_231500_KP_400.jpg").
 * @param relativePath Scoped storage folder path (e.g., "Pictures/RizzCam").
 */
data class ExportResult(
    val uri: Uri,
    val thumbnailBitmap: Bitmap,
    val displayName: String,
    val relativePath: String
)
