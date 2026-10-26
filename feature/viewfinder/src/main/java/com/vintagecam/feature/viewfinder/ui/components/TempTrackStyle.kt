package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.vintagecam.core.common.math.WhiteBalanceMath

/**
 * Styling strategies for the capsule gradient track.
 */
enum class TempTrackStyle {
    /**
     * Stylized spectrum gradient matching the reference screenshot:
     * magenta -> indigo -> cyan -> green -> yellow -> amber
     */
    SPECTRUM,

    /**
     * Physical blackbody radiation curve sampled from kelvinToRgb (blue -> white -> amber).
     */
    PHYSICAL;

    fun brush(): Brush {
        return when (this) {
            SPECTRUM -> {
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFD643C8), // Magenta
                        Color(0xFF7A5CFF), // Indigo
                        Color(0xFF3E8BFF), // Blue
                        Color(0xFF2FD4C4), // Cyan
                        Color(0xFF59E06B), // Green
                        Color(0xFFE8E34A), // Yellow
                        Color(0xFFFF9A3D)  // Amber
                    )
                )
            }
            PHYSICAL -> {
                val stops = (0..24).map { step ->
                    val offset = -100f + (step / 24f) * 200f
                    val k = WhiteBalanceMath.offsetToKelvin(offset)
                    val rgb = WhiteBalanceMath.kelvinToRgb(k)
                    Color(rgb[0], rgb[1], rgb[2], 1.0f)
                }
                Brush.horizontalGradient(stops)
            }
        }
    }
}
