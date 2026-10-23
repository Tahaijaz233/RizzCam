package com.vintagecam.feature.viewfinder.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Authentic analog camera color palette and skeuomorphic surface tokens.
 *
 * Designed to accurately replicate high-end mechanical cameras (Leica M-series, Contax T2,
 * Olympus Pen F) and retro 90s quartz-date compacts:
 * - Matte camera leatherette black body (#121214) with subtle pebbled texture.
 * - Brushed dark titanium top and bottom plates (#222226).
 * - Knurled brass accents for mechanical dials and optical bezels (#D4AF37).
 * - Retro amber quartz LCD backlight and segments (#FFB300 / #FFA726).
 * - Phosphor LED indicator green (#00E676).
 * - Mechanical shutter actuation red (#D32F2F).
 * - Warm vintage white for engraved markings and film frame borders (#F5F2EB).
 */
@Immutable
data class RetroColors(
    // Chassis & Body
    val leatheretteBlack: Color = Color(0xFF121214),
    val leatheretteDark: Color = Color(0xFF0A0A0C),
    val leatheretteTextureHighlight: Color = Color(0xFF1E1E22),
    val titaniumDark: Color = Color(0xFF222226),
    val titaniumMid: Color = Color(0xFF2C2C32),
    val titaniumHighlight: Color = Color(0xFF3E3E48),
    val chassisShadow: Color = Color(0xFF08080A),

    // Brushed Chrome & Metal Accents
    val chromeBezel: Color = Color(0xFFC5C5CB),
    val chromeHighlight: Color = Color(0xFFFFFFFF),
    val chromeShadow: Color = Color(0xFF5A5A64),
    val knurledGroove: Color = Color(0xFF1A1A1E),

    // Brass Hardware
    val knurledBrass: Color = Color(0xFFD4AF37),
    val brassHighlight: Color = Color(0xFFF6E7A9),
    val brassShadow: Color = Color(0xFF8C7322),
    val brassEngraving: Color = Color(0xFFE5C158),

    // Mechanical Shutter
    val shutterRed: Color = Color(0xFFD32F2F),
    val shutterRedPressed: Color = Color(0xFF9A1B1B),
    val shutterRedHighlight: Color = Color(0xFFFF5252),
    val shutterSocketInner: Color = Color(0xFF180808),

    // Amber Quartz LCD
    val lcdBackground: Color = Color(0xFF1F1400),
    val lcdBacklightGlow: Color = Color(0xFFFFA726),
    val lcdSegmentActive: Color = Color(0xFFFFB300),
    val lcdSegmentDim: Color = Color(0xFF473507),
    val lcdBorderBezel: Color = Color(0xFF332205),

    // Optical Reticle & Indicators
    val phosphorGreen: Color = Color(0xFF00E676),
    val focusReticleGold: Color = Color(0xFFFFD54F),
    val focusReticleSuccess: Color = Color(0xFF00E676),
    val vintageWhite: Color = Color(0xFFF5F2EB),
    val warmIvory: Color = Color(0xFFEDE8D0),
    val filmGateBorder: Color = Color(0xFF0D0D0E)
) {
    // Skeuomorphic Metallic Brushes
    val brushedTitaniumHorizontal: Brush = Brush.horizontalGradient(
        colors = listOf(titaniumDark, titaniumMid, titaniumHighlight, titaniumMid, titaniumDark)
    )

    val brushedTitaniumVertical: Brush = Brush.verticalGradient(
        colors = listOf(titaniumHighlight, titaniumMid, titaniumDark, titaniumMid)
    )

    val knurledBrassGradient: Brush = Brush.horizontalGradient(
        colors = listOf(brassShadow, knurledBrass, brassHighlight, knurledBrass, brassShadow)
    )

    val concaveShutterGradient: Brush = Brush.radialGradient(
        colors = listOf(shutterRedHighlight, shutterRed, shutterRedPressed),
        radius = 80f
    )

    val chromeBezelGradient: Brush = Brush.sweepGradient(
        colors = listOf(
            chromeHighlight,
            chromeShadow,
            chromeHighlight,
            chromeShadow,
            chromeHighlight
        )
    )

    val amberLcdGlowGradient: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2A1C02),
            Color(0xFF1F1400),
            Color(0xFF180F00)
        )
    )
}

/**
 * Skeuomorphic typography styles for engraved dials, quartz date LCD readouts,
 * and chassis badging.
 */
@Immutable
data class RetroTypography(
    // Authentic digital 7-segment quartz LCD readout style
    val lcdHeader: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        letterSpacing = 1.8.sp,
        color = Color(0xFFFFB300)
    ),
    val lcdSmall: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 1.2.sp,
        color = Color(0xFFFFB300)
    ),
    val lcdMicro: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        letterSpacing = 0.8.sp,
        color = Color(0xFFFFB300)
    ),

    // Engraved lens and dial markings (industrial, high-clarity)
    val dialMarking: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        color = Color(0xFFF5F2EB)
    ),
    val dialMarkingActive: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = Color(0xFFD4AF37)
    ),

    // Camera preset name and chassis badge
    val chassisBadge: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        letterSpacing = 2.0.sp,
        color = Color(0xFFF5F2EB)
    ),
    val presetTitle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
        color = Color(0xFFF5F2EB)
    ),
    val presetSubtitle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp,
        color = Color(0xFFB0B0B8)
    ),
    val formatTag: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.0.sp,
        color = Color(0xFFD4AF37)
    )
)

val LocalRetroColors = staticCompositionLocalOf { RetroColors() }
val LocalRetroTypography = staticCompositionLocalOf { RetroTypography() }
