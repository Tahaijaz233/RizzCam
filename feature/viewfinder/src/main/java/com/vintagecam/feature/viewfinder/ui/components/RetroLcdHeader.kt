package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vintagecam.core.camera.model.FlashMode
import com.vintagecam.domain.presets.model.AspectRatioProfile
import com.vintagecam.domain.presets.model.CameraPreset
import com.vintagecam.feature.viewfinder.theme.RetroTheme
import java.util.Locale

/**
 * Skeuomorphic amber digital quartz LCD display panel.
 *
 * Replicates the iconic top-deck and back-door status displays of late-analog compacts
 * (Contax T2, Minolta TC-1, Yashica T4 Super):
 * - Amber quartz backlight glow (#FFA726 / #FFB300) over dark recessed LCD well (#1F1400).
 * - Subtle digital segment matrix overlay.
 * - Displays active camera preset code ([KP 400], [FS 400H], [CCD 2000]).
 * - Real-time manual EV compensation readout (+0.3 EV, -1.0 EV).
 * - Film frame counter (EXP 24/36) or half-frame diptych progress (SHOT 1/2).
 * - Physical strobe status (⚡AUTO, ⚡ON, ⚡OFF) and battery gauge.
 */
@Composable
fun RetroLcdHeader(
    preset: CameraPreset,
    evCompensation: Float,
    flashMode: FlashMode,
    currentFrame: Int,
    totalFrames: Int = 36,
    isDiptychSecondShot: Boolean = false,
    isVideoMode: Boolean = false,
    isRecording: Boolean = false,
    recordingDurationMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    val colors = RetroTheme.colors
    val typography = RetroTheme.typography

    // Blinking animation for diptych half-frame wait state
    val blinkAnim = remember { Animatable(1.0f) }
    LaunchedEffect(isDiptychSecondShot) {
        if (isDiptychSecondShot) {
            blinkAnim.animateTo(
                targetValue = 0.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            blinkAnim.snapTo(1.0f)
        }
    }

    // Outer recessed LCD bezel housing
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.lcdBorderBezel)
            .border(1.dp, colors.titaniumDark, RoundedCornerShape(6.dp))
            .padding(2.5.dp)
    ) {
        // LCD glass panel with amber glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(colors.amberLcdGlowGradient)
                .border(0.8.dp, colors.lcdBackground, RoundedCornerShape(4.dp))
                .drawBehind {
                    // Micro LCD scanline / grid texture overlay
                    val step = 3.dp.toPx()
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color(0x18000000),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }
                }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Camera Preset Badge & Film Medium
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "[${preset.displayName.uppercase(Locale.ROOT)}]",
                        style = typography.lcdHeader
                    )
                    Text(
                        text = if (preset.aspectRatio == AspectRatioProfile.HALF_FRAME_DIPTYCH) {
                            "DIPTYCH 3:4"
                        } else {
                            preset.category.displayName.uppercase(Locale.ROOT)
                        },
                        style = typography.lcdMicro,
                        color = colors.lcdSegmentActive.copy(alpha = 0.85f)
                    )
                }

                // 2. Exposure EV Compensation & Shutter/ISO Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val evFormatted = if (evCompensation >= 0f) {
                        String.format(Locale.US, "+%.1f EV", evCompensation)
                    } else {
                        String.format(Locale.US, "%.1f EV", evCompensation)
                    }

                    Text(
                        text = evFormatted,
                        style = typography.lcdHeader
                    )
                    Text(
                        text = "EXP COMP",
                        style = typography.lcdMicro,
                        color = colors.lcdSegmentDim
                    )
                }

                // 3. Strobe Flash Status & Frame Counter
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Flash strobe icon & mode
                        val flashText = when (flashMode) {
                            FlashMode.AUTO -> "⚡AUTO"
                            FlashMode.ON -> "⚡ON"
                            FlashMode.OFF -> "⚡OFF"
                        }
                        Text(
                            text = flashText,
                            style = typography.lcdSmall
                        )

                        // Mechanical Battery Gauge
                        LcdBatteryGauge(
                            color = colors.lcdSegmentActive,
                            modifier = Modifier.size(width = 16.dp, height = 9.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Film frame counter, diptych step, or video recording duration
                    val frameText = if (isVideoMode) {
                        if (isRecording) {
                            val totalSec = recordingDurationMs / 1000
                            val min = totalSec / 60
                            val sec = totalSec % 60
                            String.format(Locale.US, "REC %02d:%02d", min, sec)
                        } else {
                            "STANDBY"
                        }
                    } else if (preset.aspectRatio == AspectRatioProfile.HALF_FRAME_DIPTYCH) {
                        if (isDiptychSecondShot) "SHOT 2/2" else "SHOT 1/2"
                    } else {
                        String.format(Locale.US, "EXP %02d/%02d", currentFrame, totalFrames)
                    }

                    Text(
                        text = frameText,
                        style = typography.lcdSmall,
                        color = colors.lcdSegmentActive.copy(alpha = blinkAnim.value)
                    )
                }
            }
        }
    }
}

/**
 * Skeuomorphic 3-segment LCD battery gauge graphic.
 */
@Composable
private fun LcdBatteryGauge(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.dp.toPx()

        // Battery outer shell
        drawRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(w - 2.5.dp.toPx(), h),
            style = Stroke(width = stroke)
        )

        // Battery positive terminal nipple
        drawRect(
            color = color,
            topLeft = Offset(w - 2.dp.toPx(), h * 0.25f),
            size = Size(2.dp.toPx(), h * 0.5f)
        )

        // Internal battery level bars (Full battery: 3 segments)
        val barWidth = 2.5.dp.toPx()
        val spacing = 1.5.dp.toPx()
        val startX = 2.dp.toPx()

        for (i in 0..2) {
            drawRect(
                color = color,
                topLeft = Offset(startX + i * (barWidth + spacing), 2.dp.toPx()),
                size = Size(barWidth, h - 4.dp.toPx())
            )
        }
    }
}
