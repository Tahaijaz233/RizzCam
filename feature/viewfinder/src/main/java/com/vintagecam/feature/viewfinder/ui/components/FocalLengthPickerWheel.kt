package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.width
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
import kotlin.math.roundToInt

/**
 * Continuous focal length adjustment wheel/slider (18mm to 135mm).
 * Allows fine-grained focal length adjustment and quick prime lens presets.
 */
@Composable
fun FocalLengthPickerWheel(
    currentFocalLengthMm: Int,
    onFocalLengthChanged: (Int) -> Unit,
    onDismiss: () -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier
) {
    val quickStops = listOf(18, 24, 28, 35, 50, 85, 135)

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
            // Header: Current focal length and close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FOCAL LENGTH",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Precise 1mm Stepper Group: [-] 35mm [+]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Decrement 1mm button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262626))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (currentFocalLengthMm > 18) {
                                    haptics.tick()
                                    onFocalLengthChanged(currentFocalLengthMm - 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "−",
                            color = if (currentFocalLengthMm > 18) Color.White else Color(0xFF666666),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${currentFocalLengthMm}mm",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Increment 1mm button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262626))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (currentFocalLengthMm < 135) {
                                    haptics.tick()
                                    onFocalLengthChanged(currentFocalLengthMm + 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            color = if (currentFocalLengthMm < 135) Color.White else Color(0xFF666666),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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

            // Continuous Slider (18 to 135 mm)
            Slider(
                value = currentFocalLengthMm.toFloat(),
                onValueChange = { newValue ->
                    val rounded = newValue.roundToInt()
                    if (rounded != currentFocalLengthMm) {
                        haptics.tick()
                        onFocalLengthChanged(rounded)
                    }
                },
                valueRange = 18f..135f,
                steps = 135 - 18 - 1, // 1 step per integer millimeter
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF333333)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Prime Stops Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickStops.forEach { mm ->
                    val isSelected = (mm == currentFocalLengthMm)
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
                                    onFocalLengthChanged(mm)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${mm}",
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
