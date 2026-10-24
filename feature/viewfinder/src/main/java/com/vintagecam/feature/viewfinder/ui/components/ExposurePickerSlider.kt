package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Continuous exposure compensation slider (-2.0 EV to +2.0 EV with 0.1 EV increments).
 * Matches the RizzCam vintage viewfinder design system and haptic feedback.
 */
@Composable
fun ExposurePickerSlider(
    currentEv: Float,
    onEvChanged: (Float) -> Unit,
    onDismiss: () -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier
) {
    val quickStops = listOf(-2.0f, -1.0f, -0.5f, 0.0f, 0.5f, 1.0f, 2.0f)

    fun roundEv(ev: Float): Float =
        ((ev * 10f).roundToInt() / 10f).coerceIn(-2.0f, 2.0f)

    val formattedEv = if (currentEv > 0.04f) {
        "+${String.format(Locale.US, "%.1f", currentEv)} EV"
    } else if (abs(currentEv) <= 0.04f) {
        "0.0 EV"
    } else {
        "${String.format(Locale.US, "%.1f", currentEv)} EV"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE6141414))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title, 0.1 EV Stepper [-] EV [+], and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXPOSURE",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Precise 0.1 EV Stepper Group: [-] +0.7 EV [+]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Decrement 0.1 EV button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262626))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (currentEv > -2.0f) {
                                    haptics.tick()
                                    onEvChanged(roundEv(currentEv - 0.1f))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "−",
                            color = if (currentEv > -2.0f) Color.White else Color(0xFF666666),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = formattedEv,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Increment 0.1 EV button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262626))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (currentEv < 2.0f) {
                                    haptics.tick()
                                    onEvChanged(roundEv(currentEv + 0.1f))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            color = if (currentEv < 2.0f) Color.White else Color(0xFF666666),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Continuous Slider (-2.0 to +2.0 with 0.1 step)
            Slider(
                value = currentEv.coerceIn(-2.0f, 2.0f),
                onValueChange = { newValue ->
                    val rounded = roundEv(newValue)
                    if (abs(rounded - currentEv) >= 0.09f) {
                        haptics.tick()
                        onEvChanged(rounded)
                    }
                },
                valueRange = -2.0f..2.0f,
                steps = 39, // 0.1 steps from -2.0 to +2.0
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF333333)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick EV Stops Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickStops.forEach { ev ->
                    val isSelected = abs(ev - currentEv) < 0.05f
                    val label = when {
                        ev > 0f -> "+${if (ev % 1f == 0f) ev.toInt().toString() else ev.toString()}"
                        ev < 0f -> if (ev % 1f == 0f) ev.toInt().toString() else ev.toString()
                        else -> "0"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color.White else Color(0xFF222222))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    haptics.tick()
                                    onEvChanged(ev)
                                }
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
