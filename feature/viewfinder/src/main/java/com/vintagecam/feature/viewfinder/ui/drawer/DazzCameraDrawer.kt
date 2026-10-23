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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vintagecam.domain.presets.model.AspectRatioProfile
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.model.CameraBadgeType
import com.vintagecam.feature.viewfinder.model.CameraModel
import com.vintagecam.feature.viewfinder.model.DazzCameraCatalog
import com.vintagecam.feature.viewfinder.model.OpticalAccessory
import com.vintagecam.feature.viewfinder.ui.components.CameraModelGraphic

/**
 * Quick-selection bottom drawer matching the Dazz Cam UI references.
 *
 * Components:
 * - Gradient Pro banner: "RizzCam Pro - Unlock all Cameras & Accessories"
 * - Action buttons: [SAMPLE] pill button, hamburger menu (opens full catalog), flame icon
 * - Video cameras horizontal scroll row
 * - Photo cameras horizontal scroll row
 * - Optical accessories bar (Fisheye, ND, Color Gel, Prism, Star, Aspect Ratio, FPS, Collapse)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DazzCameraDrawer(
    activeCamera: CameraModel,
    activeAccessory: OpticalAccessory,
    activeAspectRatio: AspectRatioProfile,
    onCameraSelected: (CameraModel) -> Unit,
    onAccessorySelected: (OpticalAccessory) -> Unit,
    onAspectRatioCycle: () -> Unit,
    onOpenSampleDialog: () -> Unit,
    onOpenFullCatalog: () -> Unit,
    onDismiss: () -> Unit,
    haptics: RetroHapticsEngine,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF000000),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // -----------------------------------------------------------------
            // 1. Dazz Pro / RizzCam Pro Gradient Banner
            // -----------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0D6EFD), // Electric Blue
                                Color(0xFF7B2CBF), // Deep Purple
                                Color(0xFFE63946)  // Vibrant Crimson
                            )
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptics.tick()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Swirl icon box
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚡",
                                fontSize = 18.sp
                            )
                        }

                        Column {
                            Text(
                                text = "RizzCam Pro",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Unlock all Cameras & Accessories.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Text(
                        text = "›",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // -----------------------------------------------------------------
            // 2. Action Buttons Row: [SAMPLE] pill | Hamburger ≡ | Flame 🔥
            // -----------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SAMPLE Pill Button with mini active camera graphic
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E1E20))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptics.tick()
                            onOpenSampleDialog()
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CameraModelGraphic(
                        model = activeCamera,
                        size = 18.dp
                    )
                    Text(
                        text = "SAMPLE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Hamburger ≡ Catalog Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E20))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptics.tick()
                            onOpenFullCatalog()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Catalog",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Flame 🔥 Trending Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E20))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptics.tick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Trending",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // -----------------------------------------------------------------
            // 3. Row 1: Video Cameras Horizontal Scroll List
            // -----------------------------------------------------------------
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(DazzCameraCatalog.videoCameras, key = { it.id }) { camera ->
                    val isSelected = (camera.id == activeCamera.id)
                    CameraDrawerItem(
                        camera = camera,
                        isSelected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptics.cameraSwitch()
                                onCameraSelected(camera)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -----------------------------------------------------------------
            // 4. Row 2: Photo Cameras Horizontal Scroll List
            // -----------------------------------------------------------------
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(DazzCameraCatalog.photoCameras, key = { it.id }) { camera ->
                    val isSelected = (camera.id == activeCamera.id)
                    CameraDrawerItem(
                        camera = camera,
                        isSelected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptics.cameraSwitch()
                                onCameraSelected(camera)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // -----------------------------------------------------------------
            // 5. Row 3: Optical Accessories Bar
            // -----------------------------------------------------------------
            AccessoriesBottomBar(
                activeAccessory = activeAccessory,
                activeAspectRatio = activeAspectRatio,
                onAccessorySelected = onAccessorySelected,
                onAspectRatioCycle = onAspectRatioCycle,
                onCollapse = onDismiss,
                haptics = haptics
            )
        }
    }
}

/**
 * Individual Camera Item in the drawer's horizontal lists.
 */
@Composable
private fun CameraDrawerItem(
    camera: CameraModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(68.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Camera body rendering graphic
        Box(
            modifier = Modifier
                .size(width = 62.dp, height = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            CameraModelGraphic(
                model = camera,
                size = 46.dp
            )
        }

        // Camera Name Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) Color.White else Color(0xFF141414))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            CameraNameText(
                name = camera.name,
                isSelected = isSelected
            )
        }
    }
}

/**
 * Renders camera name with authentic styling (e.g. blue "R" badge if name ends with " R").
 */
@Composable
fun CameraNameText(
    name: String,
    isSelected: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp
) {
    val defaultColor = if (isSelected) Color.Black else Color.White
    val blueRColor = Color(0xFF4A89FF)
    val redColor = Color(0xFFEF4444)

    if (name.endsWith(" R")) {
        val baseName = name.substring(0, name.length - 2)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$baseName ",
                color = defaultColor,
                fontSize = fontSize,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = "R",
                color = blueRColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    } else if (name == "Golf") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Golf",
                color = defaultColor,
                fontSize = fontSize,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    } else {
        Text(
            text = name,
            color = defaultColor,
            fontSize = fontSize,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/**
 * Row 3: Optical Accessories Bar with circular filter buttons, aspect ratio cycle, and collapse arrow.
 */
@Composable
private fun AccessoriesBottomBar(
    activeAccessory: OpticalAccessory,
    activeAspectRatio: AspectRatioProfile,
    onAccessorySelected: (OpticalAccessory) -> Unit,
    onAspectRatioCycle: () -> Unit,
    onCollapse: () -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier
) {
    val accessories = listOf(
        OpticalAccessory.FISHEYE_F,
        OpticalAccessory.ND_FILTER,
        OpticalAccessory.FLASH_C,
        OpticalAccessory.PRISM,
        OpticalAccessory.STAR
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Horizontal row of 5 circular optical accessories
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            accessories.forEach { accessory ->
                val isSelected = (accessory == activeAccessory)
                AccessoryCircleButton(
                    accessory = accessory,
                    isSelected = isSelected,
                    onClick = {
                        haptics.tick()
                        if (isSelected) {
                            onAccessorySelected(OpticalAccessory.NONE)
                        } else {
                            onAccessorySelected(accessory)
                        }
                    }
                )
            }
        }

        // Small separator dot
        Box(
            modifier = Modifier
                .size(3.dp)
                .clip(CircleShape)
                .background(Color(0xFF444444))
        )

        // Aspect ratio button [ ⛶ ]
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E20))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.tick()
                    onAspectRatioCycle()
                },
            contentAlignment = Alignment.Center
        ) {
            val ratioText = when (activeAspectRatio) {
                AspectRatioProfile.RATIO_4_3 -> "4:3"
                AspectRatioProfile.RATIO_3_2 -> "3:2"
                AspectRatioProfile.RATIO_1_1 -> "1:1"
                AspectRatioProfile.RATIO_6_7 -> "6:7"
                AspectRatioProfile.HALF_FRAME_DIPTYCH -> "1/2"
            }
            Text(
                text = ratioText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Collapse Down Arrow [ ∨ ] Button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF222224))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.tick()
                    onCollapse()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Collapse",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Circular skeuomorphic accessory button with realistic glass reflections.
 */
@Composable
private fun AccessoryCircleButton(
    accessory: OpticalAccessory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White else Color(0x33FFFFFF),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        when (accessory) {
            OpticalAccessory.FISHEYE_F, OpticalAccessory.FISHEYE_W -> {
                // Fisheye: spherical gradient with cyan/blue highlight
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF0284C7), Color(0xFF0F172A), Color.Black)
                            )
                        )
                )
            }
            OpticalAccessory.ND_FILTER -> {
                // Dark smoked glass
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF333333), Color(0xFF111111))
                            )
                        )
                )
            }
            OpticalAccessory.FLASH_C -> {
                // Red theatrical gel
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFDC2626), Color(0xFF7F1D1D), Color.Black)
                            )
                        )
                )
            }
            OpticalAccessory.PRISM -> {
                // Rainbow prism
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFEF4444),
                                    Color(0xFFF59E0B),
                                    Color(0xFF10B981),
                                    Color(0xFF3B82F6),
                                    Color(0xFF8B5CF6),
                                    Color(0xFFEF4444)
                                )
                            )
                        )
                )
            }
            OpticalAccessory.STAR -> {
                // Purple starburst
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFD946EF), Color(0xFF701A75), Color.Black)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✦", color = Color.White, fontSize = 12.sp)
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF222222)))
            }
        }
    }
}
