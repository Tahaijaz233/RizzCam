package com.vintagecam.core.common.model

import com.vintagecam.core.common.math.WhiteBalanceMath
import kotlin.math.roundToInt

/**
 * Immutable state model for live color temperature and tint balance.
 *
 * @property offset Internal temperature offset in range [-100.0, +100.0] where 0.0 is neutral 6500K.
 *                  Negative offsets produce cooler color casts (towards 10000K).
 *                  Positive offsets produce warmer color casts (towards 2000K).
 * @property tint Green-to-magenta balance offset in range [-100.0, +100.0] where 0.0 is neutral.
 * @property isAuto True when camera automatic white balance is engaged (shader strength = 0.0).
 */
data class WhiteBalanceState(
    val offset: Float = 0f,
    val tint: Float = 0f,
    val isAuto: Boolean = true
) {
    /**
     * Discrete display offset in range [-10..+10] matching reference badge (e.g. "−9").
     */
    val offsetDisplay: Int get() = (offset / 10f).roundToInt()

    /**
     * Estimated Kelvin temperature label (e.g. 5103 for "5103K −9").
     */
    val kelvinLabel: Int get() = WhiteBalanceMath.offsetToKelvin(offset).roundToInt()

    /**
     * Converts to high-speed allocation-free GPU uniform container.
     */
    fun toWbParams(): WbParams = WbParams(
        temperature = WhiteBalanceMath.offsetToKelvin(offset),
        tint = tint,
        strength = if (isAuto) 0f else 1f
    )
}
