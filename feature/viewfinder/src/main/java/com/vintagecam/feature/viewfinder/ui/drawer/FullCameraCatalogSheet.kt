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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.model.CameraModel
import com.vintagecam.feature.viewfinder.model.DazzCameraCatalog
import com.vintagecam.feature.viewfinder.model.DazzCategory
import com.vintagecam.feature.viewfinder.model.OpticalAccessory
import com.vintagecam.feature.viewfinder.ui.components.CameraModelGraphic

/**
 * Complete Full Camera Catalog modal presenting all cameras and categories in a 3-column grid.
 * Matches the reference design in WhatsApp Image 2026-10-22 at 9.59.54 PM.jpeg and parts (1), (2), (3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullCameraCatalogSheet(
    activeCamera: CameraModel,
    onCameraSelected: (CameraModel) -> Unit,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Top Header Row with Title & Close button
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CAMERA CATALOG",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222224))
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
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 1. DIGITAL Section
                item {
                    CatalogSection(
                        title = "DIGITAL",
                        cameras = DazzCameraCatalog.camerasByCategory[DazzCategory.DIGITAL] ?: emptyList(),
                        activeCamera = activeCamera,
                        onCameraSelected = {
                            haptics.cameraSwitch()
                            onCameraSelected(it)
                            onDismiss()
                        }
                    )
                }

                // 2. VIDEO Section
                item {
                    CatalogSection(
                        title = "VIDEO",
                        cameras = DazzCameraCatalog.camerasByCategory[DazzCategory.VIDEO] ?: emptyList(),
                        activeCamera = activeCamera,
                        onCameraSelected = {
                            haptics.cameraSwitch()
                            onCameraSelected(it)
                            onDismiss()
                        }
                    )
                }

                // 3. VINTAGE 135 Section
                item {
                    CatalogSection(
                        title = "VINTAGE 135",
                        cameras = DazzCameraCatalog.camerasByCategory[DazzCategory.VINTAGE_135] ?: emptyList(),
                        activeCamera = activeCamera,
                        onCameraSelected = {
                            haptics.cameraSwitch()
                            onCameraSelected(it)
                            onDismiss()
                        }
                    )
                }

                // 4. VINTAGE 120 Section
                item {
                    CatalogSection(
                        title = "VINTAGE 120",
                        cameras = DazzCameraCatalog.camerasByCategory[DazzCategory.VINTAGE_120] ?: emptyList(),
                        activeCamera = activeCamera,
                        onCameraSelected = {
                            haptics.cameraSwitch()
                            onCameraSelected(it)
                            onDismiss()
                        }
                    )
                }

                // 5. INST COLLECTION Section
                item {
                    CatalogSection(
                        title = "INST COLLECTION",
                        cameras = DazzCameraCatalog.camerasByCategory[DazzCategory.INST_COLLECTION] ?: emptyList(),
                        activeCamera = activeCamera,
                        onCameraSelected = {
                            haptics.cameraSwitch()
                            onCameraSelected(it)
                            onDismiss()
                        }
                    )
                }

                // 6. LEGACY CAMERAS Section with Disclaimer Banner
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "LEGACY CAMERAS",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        // Disclaimer Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF161618))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Legacy Cameras",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Some earlier-designed cameras may have limitations in color accuracy and dynamic range.\n\nWe recommend using newer-designed cameras to achieve better results.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // 3-Column Grid of Legacy Cameras
                        CatalogGrid(
                            cameras = DazzCameraCatalog.legacyCameras,
                            activeCamera = activeCamera,
                            onCameraSelected = {
                                haptics.cameraSwitch()
                                onCameraSelected(it)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogSection(
    title: String,
    cameras: List<CameraModel>,
    activeCamera: CameraModel,
    onCameraSelected: (CameraModel) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        CatalogGrid(
            cameras = cameras,
            activeCamera = activeCamera,
            onCameraSelected = onCameraSelected
        )
    }
}

@Composable
private fun CatalogGrid(
    cameras: List<CameraModel>,
    activeCamera: CameraModel,
    onCameraSelected: (CameraModel) -> Unit
) {
    // 3-column chunked row layout
    val rows = cameras.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (camera in row) {
                    val isSelected = (camera.id == activeCamera.id)
                    CatalogCameraCard(
                        camera = camera,
                        isSelected = isSelected,
                        onClick = { onCameraSelected(camera) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill blank slots if row has fewer than 3 items
                if (row.size < 3) {
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogCameraCard(
    camera: CameraModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(138.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141416))
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) Color.White else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        // Cyan checkmark badge at top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xFF06B6D4)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Unlocked",
                tint = Color.Black,
                modifier = Modifier.size(12.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Camera Graphic
            CameraModelGraphic(
                model = camera,
                size = 52.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Camera Name Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0C0C0E))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                CameraNameText(
                    name = camera.name,
                    isSelected = false,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom feature indicators: circle dot & frame icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle dot indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFF64748B), CircleShape)
                )

                // Frame icon indicator
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .border(1.dp, Color(0xFF64748B), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
