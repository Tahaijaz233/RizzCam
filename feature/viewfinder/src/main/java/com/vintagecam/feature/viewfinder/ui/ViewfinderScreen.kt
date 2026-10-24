package com.vintagecam.feature.viewfinder.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vintagecam.core.camera.model.FlashMode
import com.vintagecam.domain.presets.model.AspectRatioProfile
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.instant.InstantPhotoOverlay
import com.vintagecam.feature.viewfinder.model.CameraModel
import com.vintagecam.feature.viewfinder.model.CaptureMode
import com.vintagecam.feature.viewfinder.theme.RetroTheme
import com.vintagecam.feature.viewfinder.ui.components.CameraModelGraphic
import com.vintagecam.feature.viewfinder.ui.components.ExposurePickerSlider
import com.vintagecam.feature.viewfinder.ui.components.FocalLengthPickerWheel
import com.vintagecam.feature.viewfinder.ui.components.ViewfinderFramingOverlay
import com.vintagecam.feature.viewfinder.ui.dialogs.CameraSampleDialog
import com.vintagecam.feature.viewfinder.ui.drawer.DazzCameraDrawer
import com.vintagecam.feature.viewfinder.ui.drawer.FullCameraCatalogSheet
import com.vintagecam.feature.viewfinder.ui.gl.GlViewfinderSurface
import java.util.Locale
import kotlin.math.abs
import com.vintagecam.feature.viewfinder.ui.settings.ViewfinderSettingsSheet
import com.vintagecam.feature.viewfinder.viewmodel.ViewfinderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Master Viewfinder Screen reproducing the exact Dazz Cam vintage photography experience:
 * - Top Bar: Three-dots settings actuator (...) opening the settings sheet.
 * - Center Stage: 60 FPS OpenGL Viewfinder with authentic Dazz framing border, dark scrim, 3x3 grid,
 *   focal length badge, and continuous pinch/wheel focal adjustment.
 * - Floating Control Pill Group: [Thermometer (WB/tint) | Focal mm pill | Exposure EV pill].
 * - Secondary Action Bar: Double exposure, aspect ratio, self-timer, strobe flash, sensor flip.
 * - Bottom Deck: Recent photo gallery shortcut, concentric white shutter button, active camera model badge.
 * - Quick-selection drawer, full 3-column catalog, camera sample viewer, and instant print overlays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewfinderScreen(
    viewModel: ViewfinderViewModel = viewModel(),
    onNavigateToGallery: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Shutter curtain optical blackout effect
    val curtainAlpha = remember { Animatable(0f) }

    // Photo gallery import picker launcher (for 🖼️+ icon)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAndProcessPhoto(uri)
        }
    }

    LaunchedEffect(state.isCapturing) {
        if (state.isCapturing) {
            curtainAlpha.snapTo(0.95f)
            delay(70)
            curtainAlpha.animateTo(0f, tween(durationMillis = 180))
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    RetroTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // -------------------------------------------------------------
                // 1. Top Bar: Minimal with Settings (...) Actuator & Video Status
                // -------------------------------------------------------------
                DazzTopBar(
                    activeAspectRatio = state.activeCameraModel.aspectRatio,
                    onAspectRatioClick = { viewModel.cycleAspectRatio() },
                    onSettingsClick = { viewModel.openSettings() },
                    isVideoMode = state.captureMode == CaptureMode.VIDEO,
                    isRecording = state.isRecordingVideo,
                    recordingDurationMs = state.recordingDurationMs
                )

                // -------------------------------------------------------------
                // 2. Center Stage: OpenGL Preview with Dazz Framing Overlay
                // -------------------------------------------------------------
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Unified Viewfinder Optical Viewport (3:4 sensor bounds)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Live OpenGL ES Surface (Full uncropped sensor stream)
                        state.activePreset?.let { preset ->
                            GlViewfinderSurface(
                                preset = preset,
                                evCompensation = state.evCompensation,
                                showInternalOverlays = false,
                                onSurfaceProviderReady = { provider ->
                                    viewModel.bindCamera(lifecycleOwner, provider)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Skeuomorphic Framing Overlay (Dark Scrim, 3x3 Grid, White Border, mm Badge, Gestures)
                        ViewfinderFramingOverlay(
                            aspectRatio = state.activeCameraModel.aspectRatio,
                            focalLengthMm = state.focalLengthMm,
                            gridEnabled = state.gridEnabled,
                            focusState = state.focusState,
                            focusPoint = state.focusPoint,
                            onFocalLengthChanged = { mm -> viewModel.setFocalLengthMm(mm) },
                            onFocusTap = { x, y, w, h -> viewModel.onFocusTap(x, y, w, h) },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Live Retro Timestamp HUD in Viewfinder
                        val isStampActive = state.dateStampEnabled || (state.activePreset?.compositing?.enableDateStamp == true)
                        if (isStampActive) {
                            val isVideo = state.captureMode == CaptureMode.VIDEO || state.activePreset?.category == com.vintagecam.domain.presets.model.PresetCategory.RETRO_VIDEO
                            val resolvedStyle = com.vintagecam.core.camera.timestamp.RetroTimestampRenderer.resolveStyle(state.dateStampStyle, isVideo)
                            val stampText = remember(state.dateStampFormat, resolvedStyle) {
                                com.vintagecam.core.camera.timestamp.RetroTimestampRenderer.formatTimestamp(
                                    date = java.util.Date(),
                                    format = if (resolvedStyle == com.vintagecam.core.camera.timestamp.TimestampStyle.CAMCORDER_GREEN) {
                                        com.vintagecam.core.camera.timestamp.TimestampFormat.CAMCORDER_OSD
                                    } else {
                                        state.dateStampFormat
                                    }
                                )
                            }
                            val textColor = if (resolvedStyle == com.vintagecam.core.camera.timestamp.TimestampStyle.CAMCORDER_GREEN) {
                                Color(0xFF22C55E)
                            } else {
                                Color(0xFFFF9500)
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 24.dp, bottom = 48.dp)
                            ) {
                                Text(
                                    text = stampText,
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                                )
                                Text(
                                    text = stampText,
                                    color = textColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    // Shutter curtain transient blackout overlay
                    if (curtainAlpha.value > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = curtainAlpha.value))
                        )
                    }

                    // Self-timer countdown overlay
                    if (state.isSelfTimerCountingDown) {
                        Text(
                            text = "${state.countdownRemaining}",
                            color = Color.White,
                            fontSize = 88.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Floating Focal Length Picker Wheel (Pop-over)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isFocalLengthPickerOpen,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 56.dp, start = 16.dp, end = 16.dp)
                    ) {
                        FocalLengthPickerWheel(
                            currentFocalLengthMm = state.focalLengthMm,
                            onFocalLengthChanged = { mm -> viewModel.setFocalLengthMm(mm) },
                            onDismiss = { viewModel.closeFocalLengthPicker() },
                            haptics = viewModel.haptics
                        )
                    }

                    // Floating Exposure Compensation Slider (Pop-over)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isExposurePickerOpen,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 56.dp, start = 16.dp, end = 16.dp)
                    ) {
                        ExposurePickerSlider(
                            currentEv = state.evCompensation,
                            onEvChanged = { ev -> viewModel.setEvCompensation(ev) },
                            onDismiss = { viewModel.closeExposurePicker() },
                            haptics = viewModel.haptics
                        )
                    }

                    // Floating Control Pill Group: [ Thermometer | mm | EV ]
                    FloatingControlPillGroup(
                        focalLengthMm = state.focalLengthMm,
                        evCompensation = state.evCompensation,
                        onThermostatClick = { viewModel.haptics.tick() },
                        onFocalLengthClick = { viewModel.toggleFocalLengthPicker() },
                        onEvClick = { viewModel.toggleExposurePicker() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }

                // -------------------------------------------------------------
                // 3. Secondary Action Bar: 5 Iconic Tools
                // -------------------------------------------------------------
                SecondaryActionBar(
                    onImportPhoto = { photoPickerLauncher.launch("image/*") },
                    isDoubleExposureEnabled = state.isDoubleExposureEnabled,
                    selfTimerSeconds = state.selfTimerSeconds,
                    flashMode = state.flashMode,
                    onDoubleExposureToggle = { viewModel.toggleDoubleExposure() },
                    onSelfTimerCycle = { viewModel.toggleSelfTimer() },
                    onFlashToggle = { viewModel.toggleFlashMode() },
                    onCameraFlip = { viewModel.toggleCameraFacing() }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Video Recording Timer Pill (Active)
                if (state.isRecordingVideo) {
                    val totalSec = state.recordingDurationMs / 1000
                    val min = totalSec / 60
                    val sec = totalSec % 60
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xCCB91C1C),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(Locale.US, "REC %02d:%02d", min, sec),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Mode Switcher: PHOTO | VIDEO
                Row(
                    modifier = Modifier.padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PHOTO",
                        color = if (state.captureMode == CaptureMode.PHOTO) Color(0xFFFFD54F) else Color(0xFF707078),
                        fontSize = 12.sp,
                        fontWeight = if (state.captureMode == CaptureMode.PHOTO) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setCaptureMode(CaptureMode.PHOTO) }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )

                    Text(
                        text = "VIDEO",
                        color = if (state.captureMode == CaptureMode.VIDEO) Color(0xFFFFD54F) else Color(0xFF707078),
                        fontSize = 12.sp,
                        fontWeight = if (state.captureMode == CaptureMode.VIDEO) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setCaptureMode(CaptureMode.VIDEO) }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // -------------------------------------------------------------
                // 4. Bottom Deck: Gallery | Concentric Shutter | Active Camera
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Gallery Thumbnail Shortcut
                    RecentPhotoThumbnailButton(
                        thumbnailUri = state.recentThumbnailUri,
                        onClick = onNavigateToGallery,
                        haptics = viewModel.haptics
                    )

                    // Center: Concentric White Shutter Button
                    DazzShutterButton(
                        onClick = { viewModel.onShutterPressed() },
                        haptics = viewModel.haptics,
                        isVideoMode = state.captureMode == CaptureMode.VIDEO,
                        isRecording = state.isRecordingVideo,
                        isEnabled = !state.isCapturing
                    )

                    // Right: Active Camera Graphic Button
                    ActiveCameraModelButton(
                        cameraModel = state.activeCameraModel,
                        onClick = { viewModel.openPresetDrawer() },
                        haptics = viewModel.haptics
                    )
                }
            }

            // Snackbar error host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
            )

            // Dazz Camera Quick-Selection Bottom Drawer
            if (state.isPresetDrawerOpen) {
                DazzCameraDrawer(
                    activeCamera = state.activeCameraModel,
                    activeAccessory = state.activeAccessory,
                    activeAspectRatio = state.activeCameraModel.aspectRatio,
                    onCameraSelected = { camera -> viewModel.selectCameraModel(camera) },
                    onAccessorySelected = { accessory -> viewModel.selectAccessory(accessory) },
                    onAspectRatioCycle = { viewModel.cycleAspectRatio() },
                    onOpenSampleDialog = { viewModel.openSampleDialog() },
                    onOpenFullCatalog = { viewModel.openFullCatalog() },
                    onDismiss = { viewModel.closePresetDrawer() },
                    haptics = viewModel.haptics
                )
            }

            // Full 3-Column Categorized Camera Catalog Sheet
            if (state.isFullCatalogOpen) {
                FullCameraCatalogSheet(
                    activeCamera = state.activeCameraModel,
                    activeAccessory = state.activeAccessory,
                    onCameraSelected = { camera -> viewModel.selectCameraModel(camera) },
                    onAccessorySelected = { accessory -> viewModel.selectAccessory(accessory) },
                    onDismiss = { viewModel.closeFullCatalog() },
                    haptics = viewModel.haptics
                )
            }

            // Viewfinder Settings Sheet
            if (state.isSettingsOpen) {
                ViewfinderSettingsSheet(
                    gridEnabled = state.gridEnabled,
                    onGridToggle = { viewModel.setGridEnabled(it) },
                    dateStampEnabled = state.dateStampEnabled,
                    onDateStampToggle = { viewModel.setDateStampEnabled(it) },
                    dateStampStyle = state.dateStampStyle,
                    onDateStampStyleSelect = { viewModel.setDateStampStyle(it) },
                    dateStampFormat = state.dateStampFormat,
                    onDateStampFormatSelect = { viewModel.setDateStampFormat(it) },
                    activeAspectRatio = state.activeCameraModel.aspectRatio,
                    onAspectRatioSelect = { viewModel.setAspectRatio(it) },
                    soundEnabled = state.soundEnabled,
                    onSoundToggle = { viewModel.setSoundEnabled(it) },
                    hapticsEnabled = state.hapticsEnabled,
                    onHapticsToggle = { viewModel.setHapticsEnabled(it) },
                    saveOriginalEnabled = state.saveOriginalEnabled,
                    onSaveOriginalToggle = { viewModel.setSaveOriginalEnabled(it) },
                    selfTimerSeconds = state.selfTimerSeconds,
                    onSelfTimerChanged = { viewModel.setSelfTimerSeconds(it) },
                    onDismiss = { viewModel.closeSettings() },
                    haptics = viewModel.haptics
                )
            }

            // Camera Sample Photos & Specifications Dialog
            if (state.isSampleDialogOpen) {
                CameraSampleDialog(
                    camera = state.activeCameraModel,
                    onDismiss = { viewModel.closeSampleDialog() },
                    onSelectCamera = { viewModel.selectCameraModel(state.activeCameraModel) },
                    haptics = viewModel.haptics
                )
            }

            // Polaroid SX-70 Instant Photo Overlay with Chemical Development Animation
            if (state.isInstantPhotoVisible) {
                InstantPhotoOverlay(
                    photoUri = state.instantPhotoUri ?: state.recentThumbnailUri,
                    onDismissed = { viewModel.dismissInstantPhoto() }
                )
            }
        }
    }
}

/**
 * Clean top bar with aspect ratio toggle, three-dots menu actuator, and video recording indicator.
 */
@Composable
private fun DazzTopBar(
    activeAspectRatio: AspectRatioProfile,
    onAspectRatioClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isVideoMode: Boolean = false,
    isRecording: Boolean = false,
    recordingDurationMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isVideoMode) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isRecording) Color(0xCCDC2626) else Color(0x66222226)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.White else Color(0xFF94A3B8))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val statusText = if (isRecording) {
                        val totalSec = recordingDurationMs / 1000
                        val min = totalSec / 60
                        val sec = totalSec % 60
                        String.format(Locale.US, "REC %02d:%02d", min, sec)
                    } else {
                        "VIDEO"
                    }
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val ratioLabel = when (activeAspectRatio) {
                AspectRatioProfile.RATIO_9_16 -> "9:16 STORY"
                AspectRatioProfile.RATIO_16_9 -> "16:9"
                AspectRatioProfile.RATIO_4_3 -> "4:3"
                AspectRatioProfile.RATIO_3_2 -> "3:2"
                AspectRatioProfile.RATIO_1_1 -> "1:1"
                AspectRatioProfile.RATIO_6_7 -> "6:7"
                AspectRatioProfile.HALF_FRAME_DIPTYCH -> "1/2"
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (activeAspectRatio == AspectRatioProfile.RATIO_9_16 || activeAspectRatio == AspectRatioProfile.RATIO_16_9) Color(0x99F59E0B) else Color(0x66222226),
                modifier = Modifier.clickable { onAspectRatioClick() }
            ) {
                Text(
                    text = ratioLabel,
                    color = if (activeAspectRatio == AspectRatioProfile.RATIO_9_16 || activeAspectRatio == AspectRatioProfile.RATIO_16_9) Color.Black else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * Floating control pill group: [ Thermometer (WB/tint) | 35 (Focal mm) | *A +1 (EV) ].
 */
@Composable
private fun FloatingControlPillGroup(
    focalLengthMm: Int,
    evCompensation: Float,
    onThermostatClick: () -> Unit,
    onFocalLengthClick: () -> Unit,
    onEvClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xCC1A1A1A))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(22.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thermometer / WB Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { onThermostatClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DeviceThermostat,
                contentDescription = "Color Temperature / Tint",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Vertical Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(Color(0x33FFFFFF))
        )

        // Focal Length Pill (e.g. "35")
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { onFocalLengthClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$focalLengthMm",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Vertical Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(Color(0x33FFFFFF))
        )

        // EV Pill (e.g. "✳A +0.7", "✳A 0.0", "✳A -0.3")
        val evSign = if (evCompensation > 0.04f) {
            "+${String.format(Locale.US, "%.1f", evCompensation)}"
        } else if (abs(evCompensation) <= 0.04f) {
            "0.0"
        } else {
            String.format(Locale.US, "%.1f", evCompensation)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { onEvClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✳A $evSign",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Secondary 5-icon action bar matching Dazz Cam UI:
 * 1. Photo Import (🖼️+)
 * 2. Double Exposure (⧉)
 * 3. Self-Timer (⏱️)
 * 4. Strobe Flash (⚡)
 * 5. Flip Camera (🔄)
 */
@Composable
private fun SecondaryActionBar(
    onImportPhoto: () -> Unit,
    isDoubleExposureEnabled: Boolean,
    selfTimerSeconds: Int,
    flashMode: FlashMode,
    onDoubleExposureToggle: () -> Unit,
    onSelfTimerCycle: () -> Unit,
    onFlashToggle: () -> Unit,
    onCameraFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Photo Import (🖼️+)
        IconButton(
            onClick = onImportPhoto,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Import Photo",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "+",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-3).dp)
                )
            }
        }

        // 2. Double Exposure (⧉)
        IconButton(
            onClick = onDoubleExposureToggle,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = "Double Exposure",
                tint = if (isDoubleExposureEnabled) Color(0xFFFACC15) else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )
        }

        // 3. Self-Timer (⏱️)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onSelfTimerCycle() }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Self-Timer",
                    tint = if (selfTimerSeconds > 0) Color(0xFFFACC15) else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
                if (selfTimerSeconds > 0) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${selfTimerSeconds}s",
                        color = Color(0xFFFACC15),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Flash Mode (⚡)
        IconButton(
            onClick = onFlashToggle,
            modifier = Modifier.size(36.dp)
        ) {
            val (icon, tint) = when (flashMode) {
                FlashMode.AUTO -> Icons.Default.FlashAuto to Color.White.copy(alpha = 0.85f)
                FlashMode.ON -> Icons.Default.FlashOn to Color(0xFFFACC15)
                FlashMode.OFF -> Icons.Default.FlashOff to Color.White.copy(alpha = 0.45f)
            }
            Icon(
                imageVector = icon,
                contentDescription = "Flash Mode",
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }

        // 5. Flip Camera (🔄)
        IconButton(
            onClick = onCameraFlip,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Flip Camera",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Concentric white shutter button matching Dazz Cam skeuomorphic actuation button,
 * dynamically transforming into a red record ring / stop square in video mode.
 */
@Composable
fun DazzShutterButton(
    onClick: () -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier,
    isVideoMode: Boolean = false,
    isRecording: Boolean = false,
    isEnabled: Boolean = true,
    size: Dp = 76.dp
) {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch {
                            haptics.shutterClick()
                            scale.animateTo(
                                targetValue = 0.90f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                        val released = tryAwaitRelease()
                        coroutineScope.launch {
                            scale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            if (released) {
                                onClick()
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = (this.size.width / 2f) - 2.dp.toPx()
            val strokeWidth = 3.5.dp.toPx()

            val ringColor = if (isRecording) Color(0xFFFF3B30) else Color.White

            // Outer ring
            drawCircle(
                color = ringColor,
                radius = outerRadius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            if (isVideoMode) {
                if (isRecording) {
                    // Stop Square in center
                    val squareSize = (outerRadius - 8.dp.toPx()) * scale.value
                    drawRoundRect(
                        color = Color(0xFFFF3B30),
                        topLeft = Offset(center.x - squareSize / 2f, center.y - squareSize / 2f),
                        size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                } else {
                    // Red Record Circle in center
                    val innerRadius = (outerRadius - 6.dp.toPx()) * scale.value
                    drawCircle(
                        color = Color(0xFFFF3B30),
                        radius = innerRadius,
                        center = center
                    )
                }
            } else {
                // Inner solid white circle with tactile scale
                val innerRadius = (outerRadius - 6.dp.toPx()) * scale.value
                drawCircle(
                    color = Color.White,
                    radius = innerRadius,
                    center = center
                )
            }
        }
    }
}

/**
 * Recent photo thumbnail button leading to the photo gallery.
 */
@Composable
private fun RecentPhotoThumbnailButton(
    thumbnailUri: Uri?,
    onClick: () -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.5.dp, Color(0x44FFFFFF), RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptics.tick()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = "Open Gallery",
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Active camera button rendering skeuomorphic miniature camera body and opening the drawer.
 */
@Composable
private fun ActiveCameraModelButton(
    cameraModel: CameraModel,
    onClick: () -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptics.cameraSwitch()
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 62.dp, height = 46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF181818))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            CameraModelGraphic(
                model = cameraModel,
                size = 42.dp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = cameraModel.name.uppercase(),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
