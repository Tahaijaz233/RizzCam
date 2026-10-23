package com.vintagecam.core.camera.capture

import android.graphics.Bitmap
import com.vintagecam.core.camera.exif.CapturedExifMetadata

/**
 * Result data class for a captured and GL-filtered high-resolution photo.
 *
 * @param bitmap The processed, upright, retro-color-graded [Bitmap].
 * @param exifMetadata The hardware shooting parameters extracted from the original capture.
 */
data class CapturedPhotoResult(
    val bitmap: Bitmap,
    val exifMetadata: CapturedExifMetadata
)
