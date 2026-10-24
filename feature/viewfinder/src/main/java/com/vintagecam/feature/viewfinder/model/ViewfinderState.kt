package com.vintagecam.feature.viewfinder.model

import android.graphics.PointF
import android.net.Uri
import com.vintagecam.core.camera.model.FlashMode
import com.vintagecam.core.camera.model.FocusMeteringState
import com.vintagecam.core.camera.model.LensFacing
import com.vintagecam.domain.presets.model.AspectRatioProfile
import com.vintagecam.domain.presets.model.CameraPreset
import com.vintagecam.feature.viewfinder.ui.components.LensFocalLength

/**
 * State machine stages for half-frame diptych capture.
 */
enum class DiptychCaptureStep {
    IDLE,
    FIRST_FRAME_STORED,
    STITCHING
}

/**
 * Complete immutable UI state model for the vintage camera viewfinder experience.
 */
data class ViewfinderState(
    val isInitialized: Boolean = false,
    val presets: List<CameraPreset> = emptyList(),
    val activePreset: CameraPreset? = null,
    val activeCameraModel: CameraModel = DazzCameraCatalog.findById("cpm35"),
    val isPresetDrawerOpen: Boolean = false,
    val isFullCatalogOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isSampleDialogOpen: Boolean = false,
    val isFocalLengthPickerOpen: Boolean = false,
    val isExposurePickerOpen: Boolean = false,
    val focalLengthMm: Int = 35,
    val selectedLens: LensFocalLength = LensFocalLength.LENS_35MM,
    val activeAccessory: OpticalAccessory = OpticalAccessory.NONE,
    val evCompensation: Float = 1.0f,
    val flashMode: FlashMode = FlashMode.AUTO,
    val isTorchEnabled: Boolean = false,
    val hasFlashUnit: Boolean = false,
    val lensFacing: LensFacing = LensFacing.BACK,
    val isCapturing: Boolean = false,
    val isSelfTimerCountingDown: Boolean = false,
    val countdownRemaining: Int = 0,
    val selfTimerSeconds: Int = 0,
    val isDoubleExposureEnabled: Boolean = false,
    val gridEnabled: Boolean = true,
    val dateStampEnabled: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val saveOriginalEnabled: Boolean = false,
    val diptychStep: DiptychCaptureStep = DiptychCaptureStep.IDLE,
    val firstDiptychUri: Uri? = null,
    val currentFrame: Int = 1,
    val totalFrames: Int = 36,
    val recentThumbnailUri: Uri? = null,
    val focusState: FocusMeteringState = FocusMeteringState.IDLE,
    val focusPoint: PointF? = null,
    val zoomRatio: Float = 1.0f,
    val instantPhotoUri: Uri? = null,
    val isInstantPhotoVisible: Boolean = false,
    val errorMessage: String? = null
)
