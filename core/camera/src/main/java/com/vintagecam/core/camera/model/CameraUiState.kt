package com.vintagecam.core.camera.model

import android.graphics.PointF

/**
 * Physical lens hardware direction.
 */
enum class LensFacing {
    BACK,
    FRONT
}

/**
 * Still photography flash strobe triggering behavior.
 */
enum class FlashMode {
    AUTO,
    ON,
    OFF
}

/**
 * Optical tap-to-focus and auto-exposure metering state machine.
 */
enum class FocusMeteringState {
    IDLE,
    FOCUSING,
    SUCCESS,
    FAILED
}

/**
 * Immutable UI state model exposed by [com.vintagecam.core.camera.CameraController] via StateFlow.
 *
 * @param isInitialized True when CameraX provider and use cases have completed binding.
 * @param lensFacing Currently active camera sensor direction (BACK or FRONT).
 * @param flashMode Active still capture strobe setting.
 * @param isTorchEnabled True if continuous hardware illumination torch is engaged.
 * @param hasFlashUnit True if the active sensor hardware incorporates a physical flash/torch LED.
 * @param zoomRatio Current continuous optical/digital zoom magnification.
 * @param minZoomRatio Minimum sensor zoom factor reported by [androidx.camera.core.ZoomState].
 * @param maxZoomRatio Maximum supported zoom factor.
 * @param exposureCompensationIndex Integer EV compensation index set on [androidx.camera.core.CameraControl].
 * @param exposureCompensationRange Hardware-supported EV index bounds.
 * @param exposureCompensationStep EV increment per index step (e.g. 0.1666f or 0.3333f or 0.5f).
 * @param currentEv Effective EV adjustment in stops: [exposureCompensationIndex] * [exposureCompensationStep].
 * @param focusMeteringState Current optical auto-focus/auto-exposure transient state.
 * @param focusPoint Viewport coordinate normalized or physical point for tap-to-focus indicator.
 * @param errorMessage User-visible error message if camera initialization or operation fails.
 */
data class CameraUiState(
    val isInitialized: Boolean = false,
    val lensFacing: LensFacing = LensFacing.BACK,
    val flashMode: FlashMode = FlashMode.AUTO,
    val isTorchEnabled: Boolean = false,
    val hasFlashUnit: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 1.0f,
    val exposureCompensationIndex: Int = 0,
    val exposureCompensationRange: ClosedRange<Int> = 0..0,
    val exposureCompensationStep: Float = 0.0f,
    val currentEv: Float = 0.0f,
    val focusMeteringState: FocusMeteringState = FocusMeteringState.IDLE,
    val focusPoint: PointF? = null,
    val errorMessage: String? = null
)
