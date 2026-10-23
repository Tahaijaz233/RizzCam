package com.vintagecam.feature.viewfinder.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vintagecam.domain.presets.model.AspectRatioProfile
import com.vintagecam.domain.presets.model.CameraPreset
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.theme.RetroTheme
import java.util.Locale

/**
 * Skeuomorphic camera vault drawer presenting the 8 analog and digital camera models.
 *
 * Each preset card displays:
 * - Medium classification (135 Cartridge, 120 Medium Format, Peel-Apart Instant, CCD Digital).
 * - Aspect ratio profile badge (3:2, 4:3, 1:1, Diptych Half-Frame 3:4).
 * - Optical and chemical simulation characteristics (grain, halation, colorimetry).
 * - Tactile detent haptic trigger ([RetroHapticsEngine.cameraSwitch]) upon camera selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPresetDrawer(
    presets: List<CameraPreset>,
    activePreset: CameraPreset,
    onPresetSelected: (CameraPreset) -> Unit,
    onDismiss: () -> Unit,
    haptics: RetroHapticsEngine,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val colors = RetroTheme.colors
    val typography = RetroTheme.typography

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.leatheretteBlack,
        dragHandle = {
            // Brushed dark titanium drag bar
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.titaniumHighlight)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CAMERA VAULT",
                        style = typography.chassisBadge,
                        color = colors.knurledBrass
                    )
                    Text(
                        text = "Select analog film stock or optical body",
                        style = typography.presetSubtitle
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2-Column Grid of 8 Vintage Presets
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                items(presets, key = { it.id }) { preset ->
                    val isSelected = (preset.id == activePreset.id)
                    CameraPresetCard(
                        preset = preset,
                        isSelected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptics.cameraSwitch()
                                onPresetSelected(preset)
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Skeuomorphic card for an individual camera preset.
 */
@Composable
private fun CameraPresetCard(
    preset: CameraPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = RetroTheme.colors
    val typography = RetroTheme.typography

    // Characteristic accent color per preset
    val accentColor = when (preset.id) {
        "portra_400_35mm" -> Color(0xFFFFB300)      // Kodak warm amber
        "fuji_superia_half" -> Color(0xFF00E676)    // Fuji emerald
        "ccd_power_shot" -> Color(0xFF00E5FF)       // Y2K digital cyan
        "contax_t2_party" -> Color(0xFFD4AF37)      // Contax champagne gold
        "cinestill_800t" -> Color(0xFFFF1744)       // Tungsten neon red
        "holga_120n" -> Color(0xFFD500F9)           // Toy lo-fi magenta
        "polaroid_sx70" -> Color(0xFFFFFFFF)        // Polaroid clean white
        "aerochrome_ir" -> Color(0xFFFF4081)        // Infrared hot pink
        else -> colors.knurledBrass
    }

    val borderColor = if (isSelected) colors.knurledBrass else colors.titaniumDark
    val cardBackground = if (isSelected) colors.titaniumDark else colors.leatheretteDark

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardBackground)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top row: Accent color pip & Medium tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular color pip
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )

                // Aspect ratio chip
                val ratioText = when (preset.aspectRatio) {
                    AspectRatioProfile.RATIO_4_3 -> "4:3"
                    AspectRatioProfile.RATIO_3_2 -> "3:2"
                    AspectRatioProfile.RATIO_1_1 -> "1:1"
                    AspectRatioProfile.RATIO_6_7 -> "6:7"
                    AspectRatioProfile.HALF_FRAME_DIPTYCH -> "DIPTYCH"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.titaniumMid)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = ratioText,
                        style = typography.formatTag,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Camera Title
            Text(
                text = preset.displayName,
                style = typography.presetTitle,
                color = if (isSelected) colors.knurledBrass else colors.vintageWhite
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle description
            Text(
                text = preset.subtitle,
                style = typography.presetSubtitle,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom format & halation / grain specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.category.displayName.uppercase(Locale.ROOT),
                    style = typography.formatTag,
                    color = colors.chromeBezel
                )

                if (isSelected) {
                    Text(
                        text = "LOADED",
                        style = typography.formatTag,
                        color = colors.knurledBrass
                    )
                }
            }
        }
    }
}
