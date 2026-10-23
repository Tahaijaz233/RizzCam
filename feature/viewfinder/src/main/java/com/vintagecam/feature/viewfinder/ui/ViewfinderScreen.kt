package com.vintagecam.feature.viewfinder.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vintagecam.core.camera.model.FlashMode
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.model.ViewfinderState
import com.vintagecam.feature.viewfinder.theme.RetroTheme
import com.vintagecam.feature.viewfinder.ui.components.KnurledExposureDial
import com.vintagecam.feature.viewfinder.ui.components.LensSelectorWheel
import com.vintagecam.feature.viewfinder.ui.components.RetroLcdHeader
import com.vintagecam.feature.viewfinder.ui.components.SkeuomorphicShutterButton
import com.vintagecam.feature.viewfinder.ui.drawer.CameraPresetDrawer
import com.vintagecam.feature.viewfinder.ui.gl.GlViewfinderSurface
import com.vintagecam.feature.viewfinder.viewmodel.ViewfinderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Master Viewfinder Screen assembling the complete analog photography experience:
 * - Top control bar: Strobe flash mode, hardware torch, sensor flip.
 * - Retro amber LCD quartz display: active camera preset, EV compensation, frame counter, battery.
 * - Center stage: 60 FPS multi-pass OpenGL Viewfinder with dynamic aspect ratio film gate clipping.
 * - Animated golden tap-to-focus reticle and pinch-to-zoom gestures.
 * - Bottom deck: Knurled manual EV compensation dial, prime lens selector, concave mechanical shutter.
 * - Gallery thumbnail shortcut & Camera Preset Drawer modal sheet.
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

    val colors = RetroTheme.colors
    val typography = RetroTheme.typography

    // Shutter curtain optical blackout effect
    val curtainAlpha = remember { Animatable(0f) }

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
                .background(colors.leatheretteBlack)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // -------------------------------------------------------------
                // 1. Top Bar: Hardware Controls & Amber Quartz LCD
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    TopHardwareControlBar(
                        flashMode = state.flashMode,
                        isTorchOn = state.isTorchEnabled,
                        hasFlashUnit = state.hasFlashUnit,
                        onFlashToggle = { viewModel.toggleFlashMode() },
                        onTorchToggle = { viewModel.toggleTorch() },
                        onCameraFlip = { viewModel.toggleCameraFacing() },
                        onOpenVault = { viewModel.openPresetDrawer() },
                        haptics = viewModel.haptics
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Retro Amber Quartz LCD Header
                    state.activePreset?.let { preset ->
                        RetroLcdHeader(
                            preset = preset,
                            evCompensation = state.evCompensation,
                            flashMode = state.flashMode,
                            currentFrame = state.currentFrame,
                            totalFrames = state.totalFrames,
                            isDiptychSecondShot = state.diptychStep == com.vintagecam.feature.viewfinder.model.DiptychCaptureStep.FIRST_FRAME_STORED
                        )
                    }
                }

                // -------------------------------------------------------------
                // 2. Center Stage: 60 FPS Multi-Pass OpenGL Viewfinder
                // -------------------------------------------------------------
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    state.activePreset?.let { preset ->
                        GlViewfinderSurface(
                            preset = preset,
                            evCompensation = state.evCompensation,
                            focusState = state.focusState,
                            focusPoint = state.focusPoint,
                            onSurfaceProviderReady = { provider ->
                                viewModel.bindCamera(lifecycleOwner, provider)
                            },
                            onFocusTap = { x, y, w, h ->
                                viewModel.onFocusTap(x, y, w, h)
                            },
                            onZoomGesture = { delta ->
                                viewModel.onZoomGesture(delta)
                            }
                        )
                    }

                    // Shutter curtain transient blackout overlay
                    if (curtainAlpha.value > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = curtainAlpha.value))
                        )
                    }
                }

                // -------------------------------------------------------------
                // 3. Bottom Deck: Knurled Dials, Lens Wheel & Shutter Actuator
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Deck Row: Exposure Thumbwheel & Focal Length Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Knurled Manual Exposure Dial
                        KnurledExposureDial(
                            currentEv = state.evCompensation,
                            onEvChanged = { ev -> viewModel.setEvCompensation(ev) },
                            haptics = viewModel.haptics
                        )

                        // Prime Lens Selector Wheel
                        LensSelectorWheel(
                            selectedLens = state.selectedLens,
                            onLensSelected = { lens -> viewModel.selectLens(lens) },
                            haptics = viewModel.haptics
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Actuation Row: Gallery Button | Shutter Actuator | Preset Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Gallery Slide Button
                        GalleryThumbnailButton(
                            onClick = onNavigateToGallery,
                            haptics = viewModel.haptics
                        )

                        // Center: Mechanical Concave Shutter Button
                        SkeuomorphicShutterButton(
                            onClick = { viewModel.takePhoto() },
                            haptics = viewModel.haptics,
                            isEnabled = !state.isCapturing
                        )

                        // Right: Camera Model Vault Shortcut
                        CameraVaultBadgeButton(
                            presetName = state.activePreset?.displayName ?: "PRESET",
                            onClick = { viewModel.openPresetDrawer() },
                            haptics = viewModel.haptics
                        )
                    }
                }
            }

            // Snackbar error host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
            )

            // Camera Vault Modal Bottom Sheet
            if (state.isPresetDrawerOpen && state.activePreset != null) {
                CameraPresetDrawer(
                    presets = state.presets,
                    activePreset = state.activePreset!!,
                    onPresetSelected = { preset -> viewModel.selectPreset(preset) },
                    onDismiss = { viewModel.closePresetDrawer() },
                    haptics = viewModel.haptics
                )
            }

            // Polaroid SX-70 Instant Photo Overlay with Chemical Development Animation
            if (state.isInstantPhotoVisible) {
                com.vintagecam.feature.viewfinder.instant.InstantPhotoOverlay(
                    photoUri = state.instantPhotoUri ?: state.recentThumbnailUri,
                    onDismissed = { viewModel.dismissInstantPhoto() }
                )
            }
        }
    }
}

/**
 * Top control bar with brushed metal hardware switches.
 */
@Composable
private fun TopHardwareControlBar(
    flashMode: FlashMode,
    isTorchOn: Boolean,
    hasFlashUnit: Boolean,
    onFlashToggle: () -> Unit,
    onTorchToggle: () -> Unit,
    onCameraFlip: () -> Unit,
    onOpenVault: () -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier
) {
    val colors = RetroTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.titaniumDark)
            .border(1.dp, colors.titaniumHighlight, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flash Mode Switch
        IconButton(
            onClick = onFlashToggle,
            enabled = hasFlashUnit
        ) {
            val (icon, tint) = when (flashMode) {
                FlashMode.AUTO -> Icons.Default.FlashAuto to colors.knurledBrass
                FlashMode.ON -> Icons.Default.FlashOn to colors.phosphorGreen
                FlashMode.OFF -> Icons.Default.FlashOff to colors.chromeBezel.copy(alpha = 0.5f)
            }
            Icon(
                imageVector = icon,
                contentDescription = "Flash Mode",
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        // Hardware Torch Toggle
        IconButton(
            onClick = onTorchToggle,
            enabled = hasFlashUnit
        ) {
            Icon(
                imageVector = Icons.Default.Highlight,
                contentDescription = "Torch Illumination",
                tint = if (isTorchOn) colors.knurledBrass else colors.chromeBezel.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Sensor Flip (Front/Back)
        IconButton(
            onClick = onCameraFlip
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Flip Camera",
                tint = colors.vintageWhite,
                modifier = Modifier.size(22.dp)
            )
        }

        // Vault / Preset Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.titaniumMid)
                .border(0.8.dp, colors.knurledBrass, RoundedCornerShape(6.dp))
                .clickable {
                    haptics.tick()
                    onOpenVault()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "CAMERAS",
                style = RetroTheme.typography.formatTag,
                color = colors.knurledBrass
            )
        }
    }
}

/**
 * Vintage square slide mount button leading to the photo gallery.
 */
@Composable
private fun GalleryThumbnailButton(
    onClick: () -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier
) {
    val colors = RetroTheme.colors

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.titaniumDark)
            .border(1.5.dp, colors.titaniumHighlight, RoundedCornerShape(8.dp))
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
            tint = colors.vintageWhite.copy(alpha = 0.85f),
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Tactile badge button displaying active camera model name and opening the vault.
 */
@Composable
private fun CameraVaultBadgeButton(
    presetName: String,
    onClick: () -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier
) {
    val colors = RetroTheme.colors

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(colors.titaniumDark)
            .border(1.5.dp, colors.knurledBrass, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptics.cameraSwitch()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.knurledBrass)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = presetName.take(6),
                style = RetroTheme.typography.dialMarking,
                color = colors.knurledBrass
            )
        }
    }
}
