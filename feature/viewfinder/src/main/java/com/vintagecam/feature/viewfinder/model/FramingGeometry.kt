package com.vintagecam.feature.viewfinder.model

import android.graphics.Bitmap
import com.vintagecam.domain.presets.model.AspectRatioProfile
import kotlin.math.roundToInt

/**
 * Mathematical description of the viewfinder framing rectangle.
 *
 * All coordinates are in pixels relative to the container view or bitmap dimensions.
 */
data class FramingRectGeometry(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val width: Float,
    val height: Float,
    val normalizedWidth: Float,
    val normalizedHeight: Float
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * Unified mathematical helper for optical framing and high-resolution photo cropping.
 *
 * Guarantees 1:1 parity between what the photographer sees inside the viewfinder framing box
 * and what is cropped from the full-sensor camera capture.
 */
object FramingGeometryHelper {
    const val MIN_FOCAL_MM = 18
    const val MAX_FOCAL_MM = 135
    const val BASE_FOCAL_MM = 18 // Widest FOV (18mm) occupies maximum framing box area

    /**
     * Resolves the target aspect ratio (width / height) based on profile and orientation.
     */
    fun getTargetRatio(aspectRatio: AspectRatioProfile, isPortrait: Boolean = true): Float {
        val baseRatio = when (aspectRatio) {
            AspectRatioProfile.RATIO_4_3 -> 3f / 4f
            AspectRatioProfile.RATIO_3_2 -> 2f / 3f
            AspectRatioProfile.RATIO_1_1 -> 1f
            AspectRatioProfile.RATIO_6_7 -> 6f / 7f
            AspectRatioProfile.HALF_FRAME_DIPTYCH -> 3f / 4f
        }
        return if (isPortrait) baseRatio else (1f / baseRatio)
    }

    /**
     * Computes the normalized width and height [0.0..1.0] of the framing box relative to
     * container or bitmap dimensions.
     *
     * Every millimeter between 18mm and 135mm produces a strictly distinct, continuous framing scale.
     */
    fun calculateNormalizedFraming(
        containerWidth: Float,
        containerHeight: Float,
        aspectRatio: AspectRatioProfile,
        focalLengthMm: Int
    ): Pair<Float, Float> {
        val isPortrait = containerHeight >= containerWidth
        val targetRatio = getTargetRatio(aspectRatio, isPortrait)

        // Base bounding area allowing transparent scrim margin around the widest frame
        val maxBoxW = containerWidth * 0.92f
        val maxBoxH = containerHeight * 0.88f

        val (fitW, fitH) = if (maxBoxW / maxBoxH > targetRatio) {
            Pair(maxBoxH * targetRatio, maxBoxH)
        } else {
            Pair(maxBoxW, maxBoxW / targetRatio)
        }

        // Proportional optical focal length scaling:
        // Spreading focal length from 18mm to 135mm continuously scales the framing box down.
        // At 18mm: scale = 1.0 (widest box, narrow scrim margin).
        // At 35mm: scale = 18 / 35 = 0.514.
        // At 50mm: scale = 18 / 50 = 0.360.
        // At 85mm: scale = 18 / 85 = 0.212.
        // At 135mm: scale = 18 / 135 = 0.133.
        val clampedMm = focalLengthMm.coerceIn(MIN_FOCAL_MM, MAX_FOCAL_MM)
        val focalScale = (BASE_FOCAL_MM.toFloat() / clampedMm.toFloat())

        val boxW = (fitW * focalScale).coerceIn(containerWidth * 0.10f, maxBoxW)
        val boxH = (fitH * focalScale).coerceIn(containerHeight * 0.10f, maxBoxH)

        return Pair(boxW / containerWidth, boxH / containerHeight)
    }

    /**
     * Computes absolute viewport pixel geometry for on-screen UI rendering.
     */
    fun calculateViewportGeometry(
        containerWidth: Float,
        containerHeight: Float,
        aspectRatio: AspectRatioProfile,
        focalLengthMm: Int
    ): FramingRectGeometry {
        val (normW, normH) = calculateNormalizedFraming(containerWidth, containerHeight, aspectRatio, focalLengthMm)
        val boxW = containerWidth * normW
        val boxH = containerHeight * normH

        val cx = containerWidth / 2f
        val cy = containerHeight / 2f

        return FramingRectGeometry(
            left = cx - boxW / 2f,
            top = cy - boxH / 2f,
            right = cx + boxW / 2f,
            bottom = cy + boxH / 2f,
            width = boxW,
            height = boxH,
            normalizedWidth = normW,
            normalizedHeight = normH
        )
    }

    /**
     * Crops full-sensor captured [Bitmap] strictly to the active framing rectangle.
     */
    fun cropBitmapToFraming(
        bitmap: Bitmap,
        aspectRatio: AspectRatioProfile,
        focalLengthMm: Int
    ): Bitmap {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        if (bw <= 0 || bh <= 0) return bitmap

        val (normW, normH) = calculateNormalizedFraming(bw, bh, aspectRatio, focalLengthMm)
        val cropW = (bw * normW).roundToInt().coerceIn(1, bitmap.width)
        val cropH = (bh * normH).roundToInt().coerceIn(1, bitmap.height)

        val cropX = ((bitmap.width - cropW) / 2).coerceIn(0, bitmap.width - cropW)
        val cropY = ((bitmap.height - cropH) / 2).coerceIn(0, bitmap.height - cropH)

        return if (cropX == 0 && cropY == 0 && cropW == bitmap.width && cropH == bitmap.height) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
        }
    }
}
