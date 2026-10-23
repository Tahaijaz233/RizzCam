package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.theme.RetroTheme

/**
 * Optical lens focal length options modeled after classic rangefinder prime lenses.
 */
enum class LensFocalLength(
    val id: String,
    val label: String,
    val description: String,
    val zoomRatio: Float
) {
    FISHEYE(
        id = "fisheye",
        label = "FISH",
        description = "Ultra-Wide Curvilinear Fisheye",
        zoomRatio = 0.6f
    ),
    LENS_28MM(
        id = "28mm",
        label = "28mm",
        description = "Elmarit 28mm f/2.8 Wide",
        zoomRatio = 1.0f
    ),
    LENS_35MM(
        id = "35mm",
        label = "35mm",
        description = "Summicron 35mm f/2 Street",
        zoomRatio = 1.4f
    ),
    LENS_50MM(
        id = "50mm",
        label = "50mm",
        description = "Noctilux 50mm f/1.2 Portrait",
        zoomRatio = 2.0f
    )
}

/**
 * Skeuomorphic lens focal length selector wheel.
 *
 * Modeled after the rotary focal length rings on vintage multi-focal viewfinders (Leica Imarect / Zeiss 432):
 * - Brushed dark titanium pill housing with recessed chamfer bevels.
 * - Circular engraved focal length buttons with knurled brass highlights.
 * - Tactile rotary detent haptic tick ([RetroHapticsEngine.tick]) on switch.
 * - Maps directly to CameraX continuous optical/digital zoom ratios.
 */
@Composable
fun LensSelectorWheel(
    selectedLens: LensFocalLength,
    onLensSelected: (LensFocalLength) -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier
) {
    val colors = RetroTheme.colors
    val typography = RetroTheme.typography

    // Brushed titanium pill housing
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.titaniumDark)
            .border(1.dp, colors.titaniumHighlight, RoundedCornerShape(20.dp))
            .padding(3.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LensFocalLength.entries.forEach { lens ->
                val isSelected = (lens == selectedLens)

                val backgroundColor = animateColorAsState(
                    targetValue = if (isSelected) colors.knurledBrass else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "LensBgColor"
                )

                val textColor = animateColorAsState(
                    targetValue = if (isSelected) colors.leatheretteBlack else colors.vintageWhite.copy(alpha = 0.8f),
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "LensTextColor"
                )

                val borderColor = if (isSelected) colors.brassHighlight else colors.titaniumMid

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(backgroundColor.value)
                        .border(1.dp, borderColor, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptics.tick()
                                onLensSelected(lens)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lens.label,
                        style = if (isSelected) typography.dialMarkingActive else typography.dialMarking,
                        color = textColor.value
                    )
                }
            }
        }
    }
}
