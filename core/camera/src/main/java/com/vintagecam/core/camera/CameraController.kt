package com.vintagecam.core.camera

import android.content.Context
import android.graphics.PointF
import android.util.Log
import android.util.Range
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.vintagecam.core.camera.model.CameraUiState
import com.vintagecam.core.camera.model.FlashMode
import com.vintagecam.core.camera.model.FocusMeteringState
import com.vintagecam.core.camera.model.LensFacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Production-grade lifecycle-aware controller for CameraX.
 *
 * Designed specifically for vintage analog photography aesthetics:
 * - Enforces a 4:3 native aspect ratio strategy matching classic film sensors and early digicams.
 * - Supports manual exposure compensation (EV slider) within hardware bounds.
 * - Tap-to-focus and auto-exposure metering with transient 3-second auto-cancel.
 * - Flash strobe modes (AUTO, ON, OFF) and hardware torch toggle.
 * - Decouples CameraX background threads from UI state flows with thread-safe [StateFlow].
 */
class CameraController(
    private val context: Context
) {
    companion object {
        private const val TAG = "CameraController"
        private const val AUTO_CANCEL_FOCUS_SECONDS = 3L
    }

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var surfaceProvider: Preview.SurfaceProvider? = null

    var camera: Camera? = null
        private set
    var cameraControl: CameraControl? = null
        private set
    var cameraInfo: CameraInfo? = null
        private set
    var preview: Preview? = null
        private set
    var imageCapture: ImageCapture? = null
        private set
    var videoCapture: androidx.camera.video.VideoCapture<*>? = null
        private set

    private var activeLensFacing: LensFacing = LensFacing.BACK
    private var activeFlashMode: FlashMode = FlashMode.AUTO

    /**
     * Initializes CameraX asynchronously, acquiring [ProcessCameraProvider] and binding
     * the 4:3 [Preview] and [ImageCapture] use cases to the provided [LifecycleOwner].
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        initialLensFacing: LensFacing = LensFacing.BACK
    ) {
        if (!scope.isActive) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        }
        this.lifecycleOwner = lifecycleOwner
        this.surfaceProvider = surfaceProvider
        this.activeLensFacing = initialLensFacing

        scope.launch {
            try {
                val provider = ProcessCameraProvider.getInstance(context).await()
                cameraProvider = provider
                bindCameraUseCases()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to initialize CameraX", t)
                _uiState.update { it.copy(errorMessage = "Camera initialization failed: ${t.message}") }
            }
        }
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        val surfaceProv = surfaceProvider ?: return

        try {
            provider.unbindAll()

            // 1. Vintage 4:3 Aspect Ratio Strategy
            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(
                    AspectRatioStrategy(
                        AspectRatio.RATIO_4_3,
                        AspectRatioStrategy.FALLBACK_RULE_AUTO
                    )
                )
                .build()

            // 2. Preview UseCase configured with 4:3 resolution strategy
            val previewUseCase = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also {
                    it.setSurfaceProvider(surfaceProv)
                }

            // 3. ImageCapture UseCase configured for maximum still capture quality
            val captureFlashMode = when (activeFlashMode) {
                FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
            }

            val imageCaptureUseCase = ImageCapture.Builder()
                .setResolutionSelector(resolutionSelector)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(captureFlashMode)
                .build()

            // 4. CameraSelector
            val cameraSelector = if (activeLensFacing == LensFacing.BACK) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            // Check if selected camera is available on device
            if (!provider.hasCamera(cameraSelector)) {
                Log.w(TAG, "Selected camera $activeLensFacing not available, falling back to back camera")
                if (activeLensFacing != LensFacing.BACK && provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    activeLensFacing = LensFacing.BACK
                }
            }

            // 5. Bind to Lifecycle
            val useCases = mutableListOf<androidx.camera.core.UseCase>(previewUseCase, imageCaptureUseCase)
            videoCapture?.let { useCases.add(it) }

            val boundCamera = provider.bindToLifecycle(
                owner,
                cameraSelector,
                *useCases.toTypedArray()
            )

            this.camera = boundCamera
            this.cameraControl = boundCamera.cameraControl
            this.cameraInfo = boundCamera.cameraInfo
            this.preview = previewUseCase
            this.imageCapture = imageCaptureUseCase

            // 6. Observe hardware capabilities (Zoom, Torch, Exposure)
            setupCameraObservers(boundCamera.cameraInfo, owner)

            _uiState.update {
                it.copy(
                    isInitialized = true,
                    lensFacing = activeLensFacing,
                    flashMode = activeFlashMode,
                    errorMessage = null
                )
            }
            Log.i(TAG, "Camera use cases bound successfully. Lens: $activeLensFacing, AspectRatio: 4:3")
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            _uiState.update { it.copy(errorMessage = "Failed to bind camera use cases: ${e.message}") }
        }
    }

    private fun setupCameraObservers(cameraInfo: CameraInfo, owner: LifecycleOwner) {
        val hasFlash = cameraInfo.hasFlashUnit()

        // Exposure state
        val exposureState = cameraInfo.exposureState
        val range: Range<Int> = exposureState.exposureCompensationRange
        val stepRational = exposureState.exposureCompensationStep
        val stepFloat = stepRational.numerator.toFloat() / stepRational.denominator.toFloat().coerceAtLeast(1f)
        val currentIndex = exposureState.exposureCompensationIndex

        _uiState.update {
            it.copy(
                hasFlashUnit = hasFlash,
                exposureCompensationIndex = currentIndex,
                exposureCompensationRange = range.lower..range.upper,
                exposureCompensationStep = stepFloat,
                currentEv = currentIndex * stepFloat
            )
        }

        // Zoom state observer
        cameraInfo.zoomState.observe(owner, Observer { zoomState ->
            if (zoomState != null) {
                _uiState.update {
                    it.copy(
                        zoomRatio = zoomState.zoomRatio,
                        minZoomRatio = zoomState.minZoomRatio,
                        maxZoomRatio = zoomState.maxZoomRatio
                    )
                }
            }
        })

        // Torch state observer
        cameraInfo.torchState.observe(owner, Observer { torchState ->
            val isTorchOn = torchState == androidx.camera.core.TorchState.ON
            _uiState.update { it.copy(isTorchEnabled = isTorchOn) }
        })
    }

    /**
     * Toggles between [LensFacing.BACK] and [LensFacing.FRONT].
     */
    fun toggleLensFacing() {
        val nextFacing = if (activeLensFacing == LensFacing.BACK) LensFacing.FRONT else LensFacing.BACK
        setLensFacing(nextFacing)
    }

    /**
     * Explicitly sets camera sensor direction.
     */
    fun setLensFacing(lensFacing: LensFacing) {
        if (activeLensFacing == lensFacing) return
        activeLensFacing = lensFacing
        bindCameraUseCases()
    }

    /**
     * Attaches and binds a [androidx.camera.video.VideoCapture] use case for video recording.
     */
    fun attachVideoCapture(videoCapture: androidx.camera.video.VideoCapture<*>) {
        this.videoCapture = videoCapture
        bindCameraUseCases()
    }

    /**
     * Detaches the video capture use case and restores still photography only.
     */
    fun detachVideoCapture() {
        this.videoCapture = null
        bindCameraUseCases()
    }

    /**
     * Configures flash strobe mode for still capture.
     */
    fun setFlashMode(flashMode: FlashMode) {
        activeFlashMode = flashMode
        val captureFlashMode = when (flashMode) {
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = captureFlashMode
        _uiState.update { it.copy(flashMode = flashMode) }
    }

    /**
     * Toggles continuous hardware torch illumination on/off.
     */
    fun toggleTorch() {
        val currentTorch = _uiState.value.isTorchEnabled
        setTorch(!currentTorch)
    }

    /**
     * Sets continuous torch illumination state.
     */
    fun setTorch(enabled: Boolean) {
        if (!_uiState.value.hasFlashUnit) return
        val control = cameraControl ?: return
        control.enableTorch(enabled)
    }

    /**
     * Sets manual exposure compensation by integer index within hardware limits.
     */
    fun setExposureCompensationIndex(index: Int) {
        val control = cameraControl ?: return
        val range = _uiState.value.exposureCompensationRange
        val clampedIndex = index.coerceIn(range.start, range.endInclusive)

        control.setExposureCompensationIndex(clampedIndex)
        val step = _uiState.value.exposureCompensationStep
        _uiState.update {
            it.copy(
                exposureCompensationIndex = clampedIndex,
                currentEv = clampedIndex * step
            )
        }
    }

    /**
     * Sets manual exposure compensation by EV float stop (e.g. -1.0 EV, +0.67 EV).
     */
    fun setExposureEv(ev: Float) {
        val step = _uiState.value.exposureCompensationStep
        if (step <= 0.0001f) return
        val targetIndex = (ev / step).roundToInt()
        setExposureCompensationIndex(targetIndex)
    }

    /**
     * Adjusts optical / digital zoom ratio within hardware bounds.
     */
    fun setZoomRatio(ratio: Float) {
        val control = cameraControl ?: return
        val min = _uiState.value.minZoomRatio
        val max = _uiState.value.maxZoomRatio
        val clamped = ratio.coerceIn(min, max)
        control.setZoomRatio(clamped)
    }

    /**
     * Performs tap-to-focus and optical auto-exposure metering.
     * Automatically clears after [AUTO_CANCEL_FOCUS_SECONDS] seconds.
     *
     * @param x Coordinate in viewport space.
     * @param y Coordinate in viewport space.
     * @param factory [MeteringPointFactory] constructed for the active display surface.
     */
    fun focusAndMeter(x: Float, y: Float, factory: MeteringPointFactory) {
        val control = cameraControl ?: return
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(AUTO_CANCEL_FOCUS_SECONDS, TimeUnit.SECONDS)
            .build()

        _uiState.update {
            it.copy(
                focusMeteringState = FocusMeteringState.FOCUSING,
                focusPoint = PointF(x, y)
            )
        }

        val future = control.startFocusAndMetering(action)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        future.addListener({
            try {
                val result = future.get()
                _uiState.update {
                    it.copy(
                        focusMeteringState = if (result.isFocusSuccessful) {
                            FocusMeteringState.SUCCESS
                        } else {
                            FocusMeteringState.FAILED
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(focusMeteringState = FocusMeteringState.FAILED) }
            } finally {
                // Auto-clear focus indicator reticle after duration expires
                scope.launch {
                    kotlinx.coroutines.delay(AUTO_CANCEL_FOCUS_SECONDS * 1000L)
                    _uiState.update { current ->
                        if (current.focusMeteringState == FocusMeteringState.SUCCESS ||
                            current.focusMeteringState == FocusMeteringState.FAILED
                        ) {
                            current.copy(
                                focusMeteringState = FocusMeteringState.IDLE,
                                focusPoint = null
                            )
                        } else {
                            current
                        }
                    }
                }
            }
        }, mainExecutor)
    }

    /**
     * Convenience overload for tap-to-focus given viewport dimensions.
     * Constructs a [SurfaceOrientedMeteringPointFactory] automatically.
     */
    fun focusAndMeter(x: Float, y: Float, viewWidth: Float, viewHeight: Float) {
        val factory = SurfaceOrientedMeteringPointFactory(viewWidth, viewHeight)
        focusAndMeter(x, y, factory)
    }

    /**
     * Cancels any active tap-to-focus lock and restores continuous auto-focus.
     */
    fun cancelFocusAndMetering() {
        cameraControl?.cancelFocusAndMetering()
        _uiState.update {
            it.copy(
                focusMeteringState = FocusMeteringState.IDLE,
                focusPoint = null
            )
        }
    }

    /**
     * Cleans up CameraX use cases, observers, and coroutine scopes.
     */
    fun release() {
        scope.cancel()
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.w(TAG, "Error unbinding camera on release", e)
        }
        cameraProvider = null
        lifecycleOwner = null
        surfaceProvider = null
        camera = null
        cameraControl = null
        cameraInfo = null
        preview = null
        imageCapture = null
        videoCapture = null
    }
}
