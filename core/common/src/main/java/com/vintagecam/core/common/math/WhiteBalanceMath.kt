package com.vintagecam.core.common.math

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Color temperature conversion and perceptual white balance math utilities.
 *
 * Implements:
 * 1. Mired-uniform color temperature mapping (Section 6.4):
 *    Perceptually linear scale mapping internal range [-100, +100] to Kelvin [10000K, 2000K].
 *    - offset < 0: cool cast (10000K at -100)
 *    - offset = 0: neutral daylight (6500K)
 *    - offset > 0: warm cast (2000K at +100)
 * 2. Tanner Helland Kelvin-to-RGB polynomial blackbody curve fit (Section 6.3 & Appendix A).
 * 3. Rubber-band resistance curve for tactile slider dragging.
 */
object WhiteBalanceMath {

    const val NEUTRAL_KELVIN = 6500f
    const val MIN_KELVIN = 2000f
    const val MAX_KELVIN = 10000f

    const val MIN_OFFSET = -100f
    const val MAX_OFFSET = 100f

    // 1,000,000 / 6500 ≈ 153.84615 mired
    val NEUTRAL_MIRED = 1_000_000f / NEUTRAL_KELVIN
    val MIN_MIRED = 1_000_000f / MAX_KELVIN     // 100.0 mired (cool)
    val MAX_MIRED = 1_000_000f / MIN_KELVIN     // 500.0 mired (warm)

    /**
     * Maps offset in [-100..100] to Kelvin temperature in [2000..10000].
     * Negative values yield higher Kelvin (cooler blue), positive yield lower Kelvin (warmer amber).
     */
    fun offsetToKelvin(v: Float): Float {
        val clamped = v.coerceIn(MIN_OFFSET, MAX_OFFSET)
        val mired = if (clamped < 0f) {
            NEUTRAL_MIRED + (clamped / -100f) * (MIN_MIRED - NEUTRAL_MIRED)
        } else {
            NEUTRAL_MIRED + (clamped / 100f) * (MAX_MIRED - NEUTRAL_MIRED)
        }
        return 1_000_000f / mired
    }

    /**
     * Inverse mapping: converts Kelvin in [2000..10000] to offset in [-100..100].
     */
    fun kelvinToOffset(k: Float): Float {
        val clampedK = k.coerceIn(MIN_KELVIN, MAX_KELVIN)
        val mired = 1_000_000f / clampedK
        return if (mired <= NEUTRAL_MIRED) {
            val fraction = (mired - NEUTRAL_MIRED) / (MIN_MIRED - NEUTRAL_MIRED)
            (-100f * fraction).coerceIn(MIN_OFFSET, 0f)
        } else {
            val fraction = (mired - NEUTRAL_MIRED) / (MAX_MIRED - NEUTRAL_MIRED)
            (100f * fraction).coerceIn(0f, MAX_OFFSET)
        }
    }

    /**
     * Converts color temperature in Kelvin to normalized RGB color cast [0.0..1.0]
     * using the Tanner Helland blackbody approximation curve.
     */
    fun kelvinToRgb(k: Float): FloatArray {
        val t = (k.coerceIn(1000f, 40000f)) / 100.0f

        val r = if (t <= 66.0f) {
            1.0f
        } else {
            (1.292936186f * (t - 60.0f).pow(-0.1332047592f)).coerceIn(0.0f, 1.0f)
        }

        val g = if (t <= 66.0f) {
            (0.39008157876f * ln(t) - 0.63184144378f).coerceIn(0.0f, 1.0f)
        } else {
            (1.129890861f * (t - 60.0f).pow(-0.0755148492f)).coerceIn(0.0f, 1.0f)
        }

        val b = if (t >= 66.0f) {
            1.0f
        } else if (t <= 19.0f) {
            0.0f
        } else {
            (0.54320678911f * ln(t - 10.0f) - 1.19625408914f).coerceIn(0.0f, 1.0f)
        }

        return floatArrayOf(r, g, b)
    }

    /**
     * Rubber-band resistance function for touch drag overshoots.
     */
    fun rubberBand(v: Float, min: Float = MIN_OFFSET, max: Float = MAX_OFFSET, f: Float = 0.35f): Float = when {
        v < min -> min + (v - min) * f
        v > max -> max + (v - max) * f
        else -> v
    }

    /**
     * Formats signed integer offset with proper Unicode minus sign (e.g. "−9", "+3", "0").
     */
    fun formatSigned(value: Int): String = when {
        value > 0 -> "+$value"
        value < 0 -> "−${-value}"
        else -> "0"
    }
}
