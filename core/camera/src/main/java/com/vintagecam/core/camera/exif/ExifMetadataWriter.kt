package com.vintagecam.core.camera.exif

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.vintagecam.domain.presets.model.CameraPreset
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EXIF metadata orchestrator for RizzCam.
 *
 * Responsibilities:
 * 1. Telemetry Preservation: Ingests raw hardware capture EXIF (ISO, exposure time, aperture,
 *    focal length, hardware timestamp).
 * 2. Retro Emulation Tagging: Injects RizzCam branding and analog film simulation parameters
 *    into standard IPTC/EXIF headers:
 *    - [ExifInterface.TAG_MAKE]: "RizzCam"
 *    - [ExifInterface.TAG_MODEL]: "RizzCam {preset.displayName}"
 *    - [ExifInterface.TAG_SOFTWARE]: "RizzCam Retro Engine v1.0"
 *    - [ExifInterface.TAG_IMAGE_DESCRIPTION]: Film simulation recipe (LUT, grain, halation).
 * 3. File & In-Memory Serialization: Supports atomic disk files and in-memory byte streams.
 */
object ExifMetadataWriter {

    private const val TAG = "ExifMetadataWriter"
    private const val RIZZ_MAKE = "RizzCam"
    private const val RIZZ_SOFTWARE = "RizzCam Retro Engine v1.0"

    private val exifDateFormatter: SimpleDateFormat
        get() = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

    /**
     * Extracts photographic shooting telemetry from raw capture JPEG byte array.
     */
    fun extractMetadata(jpegBytes: ByteArray): CapturedExifMetadata {
        return try {
            ByteArrayInputStream(jpegBytes).use { stream ->
                val exif = ExifInterface(stream)
                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
                val dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                val dateTimeDigitized = exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                val subSec = exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL)
                val flash = exif.getAttribute(ExifInterface.TAG_FLASH)
                val whiteBalance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                val gpsLat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                val gpsLatRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
                val gpsLng = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
                val gpsLngRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
                val gpsAlt = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)
                val gpsAltRef = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF)
                val gpsMethod = exif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD)
                val gpsTime = exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP)
                val gpsDate = exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP)

                CapturedExifMetadata(
                    iso = iso,
                    exposureTime = exposureTime,
                    fNumber = fNumber,
                    focalLength = focalLength,
                    dateTime = dateTime,
                    dateTimeOriginal = dateTimeOriginal,
                    dateTimeDigitized = dateTimeDigitized,
                    subSecTimeOriginal = subSec,
                    flash = flash,
                    whiteBalance = whiteBalance,
                    orientation = orientation,
                    gpsLatitude = gpsLat,
                    gpsLatitudeRef = gpsLatRef,
                    gpsLongitude = gpsLng,
                    gpsLongitudeRef = gpsLngRef,
                    gpsAltitude = gpsAlt,
                    gpsAltitudeRef = gpsAltRef,
                    gpsProcessingMethod = gpsMethod,
                    gpsTimestamp = gpsTime,
                    gpsDateStamp = gpsDate
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse capture EXIF telemetry: ${e.message}")
            CapturedExifMetadata()
        }
    }

    /**
     * Writes branding and preserved hardware metadata into an active [ExifInterface] instance.
     */
    fun writeToExif(
        exif: ExifInterface,
        metadata: CapturedExifMetadata,
        preset: CameraPreset
    ) {
        val nowFormatted = exifDateFormatter.format(Date())

        // 1. RizzCam Branding
        exif.setAttribute(ExifInterface.TAG_MAKE, RIZZ_MAKE)
        exif.setAttribute(ExifInterface.TAG_MODEL, "RizzCam ${preset.displayName}")
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, RIZZ_SOFTWARE)

        // 2. Film Stock Recipe
        val filmDescription = buildString {
            append("Film Stock: ").append(preset.displayName)
            append(" (").append(preset.subtitle).append(")")
            append(" | Category: ").append(preset.category.displayName)
            append(" | Aspect: ").append(preset.aspectRatio.name)
            append(" | LUT: ").append(preset.lut.assetPath)
            append(" | Grain: ").append(preset.grain.intensity)
            append(" | Halation: ").append(preset.halation.enabled)
        }
        exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, filmDescription)

        // 3. Normalized Orientation (Bitmap is already upright after GL processing)
        exif.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL.toString()
        )

        // 4. Timestamps
        val dateValue = metadata.dateTimeOriginal ?: metadata.dateTime ?: nowFormatted
        exif.setAttribute(ExifInterface.TAG_DATETIME, dateValue)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateValue)
        exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, dateValue)
        metadata.subSecTimeOriginal?.let {
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, it)
        }

        // 5. Hardware Sensor Telemetry
        metadata.iso?.let {
            exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, it)
        }
        metadata.exposureTime?.let {
            exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, it)
        }
        metadata.fNumber?.let {
            exif.setAttribute(ExifInterface.TAG_F_NUMBER, it)
        }
        metadata.focalLength?.let {
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, it)
        }
        metadata.flash?.let {
            exif.setAttribute(ExifInterface.TAG_FLASH, it)
        }
        metadata.whiteBalance?.let {
            exif.setAttribute(ExifInterface.TAG_WHITE_BALANCE, it)
        }

        // 6. GPS Geolocation Telemetry
        metadata.gpsLatitude?.let { exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, it) }
        metadata.gpsLatitudeRef?.let { exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, it) }
        metadata.gpsLongitude?.let { exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, it) }
        metadata.gpsLongitudeRef?.let { exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, it) }
        metadata.gpsAltitude?.let { exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, it) }
        metadata.gpsAltitudeRef?.let { exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, it) }
        metadata.gpsProcessingMethod?.let { exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, it) }
        metadata.gpsTimestamp?.let { exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, it) }
        metadata.gpsDateStamp?.let { exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, it) }
    }

    /**
     * Injects metadata into an existing JPEG file on disk and saves changes atomically.
     */
    fun writeToFile(
        file: File,
        metadata: CapturedExifMetadata,
        preset: CameraPreset
    ) {
        try {
            val exif = ExifInterface(file)
            writeToExif(exif, metadata, preset)
            exif.saveAttributes()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing EXIF metadata to file ${file.absolutePath}", e)
        }
    }

    /**
     * Injects metadata into an in-memory JPEG byte array and returns the tagged byte array.
     */
    fun writeToByteArray(
        jpegBytes: ByteArray,
        metadata: CapturedExifMetadata,
        preset: CameraPreset
    ): ByteArray {
        var tempFile: File? = null
        return try {
            tempFile = File.createTempFile("rizz_exif_", ".jpg")
            FileOutputStream(tempFile).use { it.write(jpegBytes) }

            val exif = ExifInterface(tempFile)
            writeToExif(exif, metadata, preset)
            exif.saveAttributes()

            tempFile.readBytes()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject EXIF into byte array", e)
            jpegBytes
        } finally {
            tempFile?.delete()
        }
    }
}
