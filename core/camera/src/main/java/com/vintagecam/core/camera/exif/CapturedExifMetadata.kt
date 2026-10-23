package com.vintagecam.core.camera.exif

import androidx.exifinterface.media.ExifInterface

/**
 * Encapsulates photographic shooting metadata captured from the hardware sensor.
 *
 * Preserves hardware telemetry (ISO, exposure shutter speed, aperture f-stop, focal length)
 * so it can be re-injected into the final retro-graded output photo.
 */
data class CapturedExifMetadata(
    val iso: String? = null,
    val exposureTime: String? = null,
    val fNumber: String? = null,
    val focalLength: String? = null,
    val dateTime: String? = null,
    val dateTimeOriginal: String? = null,
    val dateTimeDigitized: String? = null,
    val subSecTimeOriginal: String? = null,
    val flash: String? = null,
    val whiteBalance: String? = null,
    val orientation: Int = ExifInterface.ORIENTATION_NORMAL,
    val gpsLatitude: String? = null,
    val gpsLatitudeRef: String? = null,
    val gpsLongitude: String? = null,
    val gpsLongitudeRef: String? = null,
    val gpsAltitude: String? = null,
    val gpsAltitudeRef: String? = null,
    val gpsProcessingMethod: String? = null,
    val gpsTimestamp: String? = null,
    val gpsDateStamp: String? = null
)
