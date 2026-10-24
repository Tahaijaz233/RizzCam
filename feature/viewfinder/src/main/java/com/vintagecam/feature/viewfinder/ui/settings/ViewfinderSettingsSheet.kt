package com.vintagecam.feature.viewfinder.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vintagecam.core.camera.timestamp.TimestampFormat
import com.vintagecam.core.camera.timestamp.TimestampStyle
import com.vintagecam.domain.presets.model.AspectRatioProfile
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine

/**
 * Skeuomorphic Settings bottom sheet opened from the top '...' menu on the viewfinder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewfinderSettingsSheet(
    gridEnabled: Boolean,
    onGridToggle: (Boolean) -> Unit,
    dateStampEnabled: Boolean,
    onDateStampToggle: (Boolean) -> Unit,
    dateStampStyle: TimestampStyle = TimestampStyle.AUTO,
    onDateStampStyleSelect: (TimestampStyle) -> Unit = {},
    dateStampFormat: TimestampFormat = TimestampFormat.YEAR_MONTH_DAY,
    onDateStampFormatSelect: (TimestampFormat) -> Unit = {},
    activeAspectRatio: AspectRatioProfile = AspectRatioProfile.RATIO_3_2,
    onAspectRatioSelect: (AspectRatioProfile) -> Unit = {},
    soundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsToggle: (Boolean) -> Unit,
    saveOriginalEnabled: Boolean,
    onSaveOriginalToggle: (Boolean) -> Unit,
    selfTimerSeconds: Int,
    onSelfTimerChanged: (Int) -> Unit,
    onDismiss: () -> Unit,
    haptics: RetroHapticsEngine,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141416),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SETTINGS",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262628))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Section 1: Camera Toggles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1E22))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SettingsToggleRow(
                    title = "Rule of Thirds Grid",
                    subtitle = "Display 3x3 composition guidelines in viewfinder",
                    isChecked = gridEnabled,
                    onCheckedChange = {
                        haptics.tick()
                        onGridToggle(it)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                SettingsToggleRow(
                    title = "Film Date Stamp",
                    subtitle = "Imprint retro orange LED quartz date on captured photos",
                    isChecked = dateStampEnabled,
                    onCheckedChange = {
                        haptics.tick()
                        onDateStampToggle(it)
                    }
                )

                if (dateStampEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "TIMESTAMP STYLE",
                        color = Color(0xFFF59E0B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair(TimestampStyle.AUTO, "Auto"),
                            Pair(TimestampStyle.DIGICAM_ORANGE, "Orange Quartz"),
                            Pair(TimestampStyle.CAMCORDER_GREEN, "VHS Green"),
                            Pair(TimestampStyle.RETRO_GOLD, "Gold")
                        ).forEach { (style, label) ->
                            val isSelected = (dateStampStyle == style)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFF59E0B) else Color(0xFF2A2A2E))
                                    .clickable {
                                        haptics.tick()
                                        onDateStampStyleSelect(style)
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DATE FORMAT",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair(TimestampFormat.YEAR_MONTH_DAY, "'YY MM DD"),
                            Pair(TimestampFormat.MONTH_DAY_YEAR, "MM DD 'YY"),
                            Pair(TimestampFormat.DATE_WITH_TIME, "With Time")
                        ).forEach { (fmt, label) ->
                            val isSelected = (dateStampFormat == fmt)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF3B82F6) else Color(0xFF2A2A2E))
                                    .clickable {
                                        haptics.tick()
                                        onDateStampFormatSelect(fmt)
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                SettingsToggleRow(
                    title = "Mechanical Shutter Sound",
                    subtitle = "Audible analog leaf shutter and mirror release sound",
                    isChecked = soundEnabled,
                    onCheckedChange = {
                        haptics.tick()
                        onSoundToggle(it)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                SettingsToggleRow(
                    title = "Haptic Detents",
                    subtitle = "Tactile mechanical vibration on dials and shutter release",
                    isChecked = hapticsEnabled,
                    onCheckedChange = {
                        haptics.tick()
                        onHapticsToggle(it)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                SettingsToggleRow(
                    title = "Save Original Photo",
                    subtitle = "Export unfiltered raw camera sensor frame alongside vintage print",
                    isChecked = saveOriginalEnabled,
                    onCheckedChange = {
                        haptics.tick()
                        onSaveOriginalToggle(it)
                    }
                )
            }

            // Section 2: Self-Timer Duration
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1E22))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SELF-TIMER DURATION",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Pair(0, "Off"),
                        Pair(3, "3 Seconds"),
                        Pair(10, "10 Seconds")
                    ).forEach { (seconds, label) ->
                        val isSelected = (seconds == selfTimerSeconds)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color.White else Color(0xFF2B2B30))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptics.tick()
                                    onSelfTimerChanged(seconds)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Section 3: Aspect Ratio / Story Mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1E22))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ASPECT RATIO & STORY FRAMING",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair(AspectRatioProfile.RATIO_9_16, "9:16 Story"),
                        Pair(AspectRatioProfile.RATIO_3_2, "3:2 Film"),
                        Pair(AspectRatioProfile.RATIO_4_3, "4:3 Digicam"),
                        Pair(AspectRatioProfile.RATIO_1_1, "1:1 Square")
                    ).forEach { (ratio, label) ->
                        val isSelected = (activeAspectRatio == ratio)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFF59E0B) else Color(0xFF26262A))
                                .clickable {
                                    haptics.tick()
                                    onAspectRatioSelect(ratio)
                                }
                                .padding(vertical = 9.dp),
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

            // About Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "RizzCam Analog Camera Engine",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Version 1.0.0 • Dazz Inspired Architecture",
                    color = Color(0xFF475569),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0D6EFD),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF333338)
            )
        )
    }
}
