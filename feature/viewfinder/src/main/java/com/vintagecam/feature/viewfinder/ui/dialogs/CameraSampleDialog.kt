package com.vintagecam.feature.viewfinder.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.model.CameraModel
import com.vintagecam.feature.viewfinder.model.DazzCameraCatalog
import com.vintagecam.feature.viewfinder.ui.components.CameraModelGraphic

/**
 * Sample photos dialog presenting vintage photography specs, sample mockups,
 * and shooting tips for the selected camera.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSampleDialog(
    camera: CameraModel,
    onDismiss: () -> Unit,
    onSelectCamera: () -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier
) {
    val sampleInfo = remember(camera) { DazzCameraCatalog.getSampleInfo(camera) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141416))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Camera name & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CameraModelGraphic(model = camera, size = 32.dp)
                    Column {
                        Text(
                            text = camera.name,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = camera.category.displayName,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF26262A))
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

            Spacer(modifier = Modifier.height(16.dp))

            // Sample Photo Mockup Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFEA580C),
                                Color(0xFF7C2D12),
                                Color(0xFF1C1917)
                            )
                        )
                    )
                    .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.BottomStart
            ) {
                // Gradient dark scrim at bottom of photo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xDD000000))
                            )
                        )
                )

                Text(
                    text = sampleInfo.sampleCaption,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Specification Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1E22))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecRow(label = "Medium", value = sampleInfo.sensorType)
                SpecRow(label = "Sensitivity", value = sampleInfo.iso)
                SpecRow(label = "Dynamic Range", value = sampleInfo.dynamicRange)
                SpecRow(label = "Film Grain", value = sampleInfo.grainProfile)
                SpecRow(label = "Optical Bloom", value = sampleInfo.halationCharacteristics)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shooting Tip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(12.dp)
            ) {
                Text(
                    text = "💡 ${sampleInfo.bestLighting}",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Button: "SELECT THIS CAMERA"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptics.shutterClick()
                        onSelectCamera()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SELECT THIS CAMERA",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif
        )
    }
}
