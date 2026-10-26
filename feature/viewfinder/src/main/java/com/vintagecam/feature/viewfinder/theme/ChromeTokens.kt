package com.vintagecam.feature.viewfinder.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * iOS-grade camera chrome design tokens (Section 5).
 * Tier 1 glassmorphism approximates Apple HIG translucent materials with specular edge highlights.
 */
object ChromeTokens {
    // Materials (Tier 1)
    val PillLightFill    = Color.White.copy(alpha = 0.78f)   // iOS light-material approx
    val PillDarkFill     = Color.Black.copy(alpha = 0.42f)
    val CircleFill       = Color.White.copy(alpha = 0.14f)
    val CircleFillActive = Color.White.copy(alpha = 0.28f)
    val HairlineTop      = Color.White.copy(alpha = 0.55f)   // specular top edge
    val HairlineBottom   = Color.White.copy(alpha = 0.16f)
    val ChromeBlack      = Color.Black                        // letterbox, never #111 etc.

    // Active outline
    val ActiveRingColor  = Color.White.copy(alpha = 0.45f)

    // Shape & stroke
    val PillRadius       = 100.dp          // capsule
    val CardRadius       = 22.dp
    val Hairline         = 0.8.dp
    val KnobD            = 18.dp
    val TrackH           = 14.dp

    // Type
    val LabelMono        = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White.copy(alpha = 0.60f)
    )
    val PillText         = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.Black
    )
}

/**
 * iOS-grade spring and timing curves for docked "sticky" camera chrome (Section 5 & Appendix C).
 */
object Motion {
    /** Panel expand/collapse — iOS sheet-ish: slight overshoot, fast settle. */
    val PanelSpring: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 340f)

    /** Pill/badge pop & matched-geometry morph. */
    val PillSpring: SpringSpec<Float> = spring(dampingRatio = 0.72f, stiffness = 520f)

    /** Press-down scale (0.88) — instant in, spring out = "sticky/weighty". */
    val PressIn: TweenSpec<Float> = tween(durationMillis = 90)
    val PressOut: SpringSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 700f)

    /** Fade transition on chrome collapse. */
    val ChromeFade: SpringSpec<Float> = spring(dampingRatio = 1.0f, stiffness = 200f)
}

/**
 * Glass pill modifier applying Tier 1 translucent fill and dual-tone specular hairline border.
 */
fun Modifier.glassPill(dark: Boolean = false): Modifier = this
    .background(
        if (dark) ChromeTokens.PillDarkFill else ChromeTokens.PillLightFill,
        RoundedCornerShape(ChromeTokens.PillRadius)
    )
    .border(
        ChromeTokens.Hairline,
        Brush.verticalGradient(listOf(ChromeTokens.HairlineTop, ChromeTokens.HairlineBottom)),
        RoundedCornerShape(ChromeTokens.PillRadius)
    )
    .materialVibrancyHook()

/**
 * Saturation lift / vibrancy hook (reserved for Tier 3 RenderEffect / shader integration).
 */
fun Modifier.materialVibrancyHook(): Modifier = this
