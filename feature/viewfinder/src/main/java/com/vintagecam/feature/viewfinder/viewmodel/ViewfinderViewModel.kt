package com.vintagecam.feature.viewfinder.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.vintagecam.core.camera.CameraController
import com.vintagecam.core.camera.capture.HighResCaptureManager
import com.vintagecam.core.camera.diptych.DiptychResult
import com.vintagecam.core.camera.diptych.HalfFrameDiptychManager
import com.vintagecam.core.camera.export.MediaStoreExporter
import com.vintagecam.core.camera.model.FlashMode
import com.vintagecam.core.camera.model.FocusMeteringState
import com.vintagecam.core.camera.model.LensFacing
import com.vintagecam.core.gl.VintageGlPipeline
import com.vintagecam.core.gl.egl.GlThreadExecutor
import com.vintagecam.core.gl.texture.TextureCacheManager
import com.vintagecam.domain.presets.model.AspectRatioProfile
import com.vintagecam.domain.presets.model.CameraPreset
import com.vintagecam.domain.presets.repository.PresetRepository
import com.vintagecam.domain.presets.repository.PresetRepositoryImpl
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.model.DiptychCaptureStep
import com.vintagecam.feature.viewfinder.model.ViewfinderState
import com.vintagecam.feature.viewfinder.ui.components.LensFocalLength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Production ViewModel managing the complete analog camera viewfinder experience.
 *
 * Responsibilities:
 * - Orchestrates CameraX lifecycle and state flow updates from [CameraController].
 * - Loads and maintains active analog film stock presets via [PresetRepository].
 * - Implements the half-frame diptych capture state machine (shot 1/2 stored, shot 2/2 stitch).
 * - Manages physical lens focal length switching (28mm, 35mm, 50mm, Fisheye).
 * - Controls manual exposure compensation (-2.0 EV to +2.0 EV).
 * - Handles high-resolution shutter release, offscreen 6-pass GL rendering, and MediaStore export.
 */
class ViewfinderViewModel(
    application: Application,
    private val presetRepository: PresetRepository = PresetRepositoryImpl(application.applicationContext),
    val cameraController: CameraController = CameraController(application.applicationContext),
    val haptics: RetroHapticsEngine = RetroHapticsEngine(application.applicationContext)
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ViewfinderViewModel"
    }

    private val mediaStoreExporter = MediaStoreExporter(application.applicationContext)
    private val diptychManager = HalfFrameDiptychManager()

    private val captureGlExecutor = GlThreadExecutor("CaptureGlThread")
    private var capturePipeline: VintageGlPipeline? = null
    private val captureTextureCache = TextureCacheManager()

    private val _state = MutableStateFlow(ViewfinderState())
    val state: StateFlow<ViewfinderState> = _state.asStateFlow()

    private val _lastCapturedPhotoUri = MutableStateFlow<Uri?>(null)
    val lastCapturedPhotoUri: StateFlow<Uri?> = _lastCapturedPhotoUri.asStateFlow()

    private var activeLifecycleOwner: LifecycleOwner? = null
    private var activeSurfaceProvider: Preview.SurfaceProvider? = null

    init {
        loadPresets()
        observeCameraState()
    }

    private fun ensureCapturePipelineInitialized() {
        if (capturePipeline == null) {
            captureGlExecutor.postAndWait {
                if (capturePipeline == null) {
                    capturePipeline = VintageGlPipeline.createFromAssets(getApplication<Application>().assets)
                }
            }
        }
    }

    private fun loadPresets() {
        viewModelScope.launch {
            try {
                val presets = presetRepository.getAllPresets()
                val defaultPreset = presets.firstOrNull { it.id == "portra_400_35mm" }
                    ?: presets.firstOrNull()

                _state.update {
                    it.copy(
                        presets = presets,
                        activePreset = defaultPreset
                    )
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load presets", t)
                _state.update { it.copy(errorMessage = "Failed to load camera presets: ${t.message}") }
            }
        }
    }

    private fun observeCameraState() {
        viewModelScope.launch {
            cameraController.uiState.collect { cameraUi ->
                _state.update {
                    it.copy(
                        isInitialized = cameraUi.isInitialized,
                        flashMode = cameraUi.flashMode,
                        isTorchEnabled = cameraUi.isTorchEnabled,
                        hasFlashUnit = cameraUi.hasFlashUnit,
                        lensFacing = cameraUi.lensFacing,
                        evCompensation = cameraUi.currentEv,
                        zoomRatio = cameraUi.zoomRatio,
                        focusState = cameraUi.focusMeteringState,
                        focusPoint = cameraUi.focusPoint,
                        errorMessage = cameraUi.errorMessage ?: it.errorMessage
                    )
                }
            }
        }
    }

    /**
     * Binds CameraX use cases to the UI [LifecycleOwner] and the OpenGL [Preview.SurfaceProvider].
     */
    fun bindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        this.activeLifecycleOwner = lifecycleOwner
        this.activeSurfaceProvider = surfaceProvider
        cameraController.startCamera(lifecycleOwner, surfaceProvider, _state.value.lensFacing)
    }

    /**
     * Selects an analog film stock or optical camera preset from the vault.
     */
    fun selectPreset(preset: CameraPreset) {
        val currentPreset = _state.value.activePreset
        if (currentPreset?.id != preset.id || preset.aspectRatio != AspectRatioProfile.HALF_FRAME_DIPTYCH) {
            diptychManager.cancelPendingFrame()
        }

        _state.update { current ->
            // Reset diptych sequence if moving away from half-frame
            val resetDiptych = if (preset.aspectRatio != AspectRatioProfile.HALF_FRAME_DIPTYCH) {
                DiptychCaptureStep.IDLE
            } else {
                current.diptychStep
            }
            current.copy(
                activePreset = preset,
                diptychStep = resetDiptych,
                firstDiptychUri = if (resetDiptych == DiptychCaptureStep.IDLE) null else current.firstDiptychUri
            )
        }
    }

    fun openPresetDrawer() {
        _state.update { it.copy(isPresetDrawerOpen = true) }
    }

    fun closePresetDrawer() {
        _state.update { it.copy(isPresetDrawerOpen = false) }
    }

    /**
     * Changes optical focal length simulation and sets CameraX zoom ratio.
     */
    fun selectLens(lens: LensFocalLength) {
        _state.update { it.copy(selectedLens = lens) }
        cameraController.setZoomRatio(lens.zoomRatio)
    }

    /**
     * Adjusts manual exposure compensation in EV stops.
     */
    fun setEvCompensation(ev: Float) {
        _state.update { it.copy(evCompensation = ev) }
        cameraController.setExposureEv(ev)
    }

    /**
     * Cycles flash strobe modes (AUTO -> ON -> OFF -> AUTO).
     */
    fun toggleFlashMode() {
        val nextMode = when (_state.value.flashMode) {
            FlashMode.AUTO -> FlashMode.ON
            FlashMode.ON -> FlashMode.OFF
            FlashMode.OFF -> FlashMode.AUTO
        }
        haptics.tick()
        cameraController.setFlashMode(nextMode)
    }

    /**
     * Toggles hardware torch illumination.
     */
    fun toggleTorch() {
        haptics.tick()
        cameraController.toggleTorch()
    }

    /**
     * Flips between front and back camera sensors.
     */
    fun toggleCameraFacing() {
        haptics.cameraSwitch()
        cameraController.toggleLensFacing()
    }

    /**
     * Dispatches optical tap-to-focus and auto-exposure metering.
     */
    fun onFocusTap(x: Float, y: Float, viewWidth: Float, viewHeight: Float) {
        haptics.tick()
        cameraController.focusAndMeter(x, y, viewWidth, viewHeight)
    }

    /**
     * Handles pinch-to-zoom gesture scale delta.
     */
    fun onZoomGesture(zoomDelta: Float) {
        val currentRatio = _state.value.zoomRatio
        val targetRatio = currentRatio * zoomDelta
        cameraController.setZoomRatio(targetRatio)
    }

    /**
     * Triggers mechanical shutter release, coordinating half-frame diptych sequences
     * or standard analog photo capture.
     */
    fun takePhoto(onPhotoCaptured: ((Uri?) -> Unit)? = null) {
        val activePreset = _state.value.activePreset ?: return
        if (_state.value.isCapturing) return

        viewModelScope.launch(Dispatchers.Default) {
            _state.update { it.copy(isCapturing = true) }

            try {
                if (activePreset.aspectRatio == AspectRatioProfile.HALF_FRAME_DIPTYCH) {
                    handleHalfFrameCapture(onPhotoCaptured)
                } else {
                    handleStandardCapture(onPhotoCaptured)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Capture execution failed", t)
                _state.update { it.copy(errorMessage = "Photo capture failed: ${t.message}") }
            } finally {
                _state.update { it.copy(isCapturing = false) }
            }
        }
    }

    private suspend fun executeCaptureAndFilter(
        imageCapture: ImageCapture,
        preset: CameraPreset,
        isFrontCamera: Boolean,
        ev: Float
    ): com.vintagecam.core.camera.capture.CapturedPhotoResult {
        ensureCapturePipelineInitialized()
        val pipeline = capturePipeline ?: throw IllegalStateException("Capture pipeline not available")
        val assets = getApplication<Application>().assets

        val (lutId, leakId, borderId) = captureGlExecutor.postWithResult {
            val lut = captureTextureCache.getOrLoadLut3d(assets, preset.lut.assetPath, preset.lut.lutSize)
            val leak = captureTextureCache.getOrLoadLightLeak(assets, preset.compositing.lightLeakPool.firstOrNull())
            val border = captureTextureCache.getOrLoadBorderMask(assets, preset.compositing.borderMaskAsset)
            Triple(lut, leak, border)
        }

        val captureManager = HighResCaptureManager(
            context = getApplication<Application>().applicationContext,
            glThreadExecutor = captureGlExecutor,
            pipeline = pipeline
        )

        return captureManager.capturePhoto(
            imageCapture = imageCapture,
            preset = preset,
            lutTextureId = lutId,
            lightLeakTextureId = leakId,
            borderMaskTextureId = borderId,
            exposureEv = ev,
            isFrontCamera = isFrontCamera
        )
    }

    private suspend fun handleHalfFrameCapture(onPhotoCaptured: ((Uri?) -> Unit)?) {
        val imageCapture = cameraController.imageCapture
            ?: throw IllegalStateException("Camera ImageCapture use case is not bound")
        val activePreset = _state.value.activePreset
            ?: throw IllegalStateException("No active camera preset selected")

        val isFront = _state.value.lensFacing == LensFacing.FRONT
        val ev = _state.value.evCompensation
        val captureResult = executeCaptureAndFilter(imageCapture, activePreset, isFront, ev)

        when (val diptychResult = diptychManager.processFrame(captureResult.bitmap)) {
            is DiptychResult.FirstFrameStored -> {
                haptics.diptychAdvance()
                _state.update {
                    it.copy(
                        diptychStep = DiptychCaptureStep.FIRST_FRAME_STORED,
                        recentThumbnailBitmap = diptychResult.firstFrameThumbnail
                    )
                }
                onPhotoCaptured?.invoke(null)
            }
            is DiptychResult.CompositeReady -> {
                haptics.shutterClick()
                _state.update { it.copy(diptychStep = DiptychCaptureStep.STITCHING) }

                try {
                    val exportResult = mediaStoreExporter.exportPhoto(
                        bitmap = diptychResult.compositeBitmap,
                        preset = activePreset,
                        metadata = captureResult.exifMetadata
                    )

                    advanceFrameCounter()

                    _lastCapturedPhotoUri.value = exportResult.uri

                    _state.update {
                        it.copy(
                            diptychStep = DiptychCaptureStep.IDLE,
                            firstDiptychUri = null,
                            recentThumbnailBitmap = exportResult.thumbnailBitmap,
                            recentThumbnailUri = exportResult.uri
                        )
                    }
                    onPhotoCaptured?.invoke(exportResult.uri)
                } finally {
                    if (!diptychResult.compositeBitmap.isRecycled) {
                        diptychResult.compositeBitmap.recycle()
                    }
                }
            }
        }
    }

    private suspend fun handleStandardCapture(onPhotoCaptured: ((Uri?) -> Unit)?) {
        val imageCapture = cameraController.imageCapture
            ?: throw IllegalStateException("Camera ImageCapture use case is not bound")
        val activePreset = _state.value.activePreset
            ?: throw IllegalStateException("No active camera preset selected")

        // Mechanical shutter release click
        haptics.shutterClick()

        val isFront = _state.value.lensFacing == LensFacing.FRONT
        val ev = _state.value.evCompensation
        val captureResult = executeCaptureAndFilter(imageCapture, activePreset, isFront, ev)

        try {
            val exportResult = mediaStoreExporter.exportPhoto(
                bitmap = captureResult.bitmap,
                preset = activePreset,
                metadata = captureResult.exifMetadata
            )

            // Advance exposure counter (e.g., 24 -> 25)
            advanceFrameCounter()

            _lastCapturedPhotoUri.value = exportResult.uri

            _state.update {
                it.copy(
                    recentThumbnailBitmap = exportResult.thumbnailBitmap,
                    recentThumbnailUri = exportResult.uri
                )
            }

            if (activePreset.id.contains("polaroid", ignoreCase = true) || activePreset.id.contains("sx70", ignoreCase = true)) {
                _state.update {
                    it.copy(
                        isInstantPhotoVisible = true,
                        instantPhotoUri = exportResult.uri
                    )
                }
            }

            onPhotoCaptured?.invoke(exportResult.uri)
        } finally {
            if (!captureResult.bitmap.isRecycled) {
                captureResult.bitmap.recycle()
            }
        }
    }

    fun showInstantPhoto(uri: Uri? = null) {
        _state.update {
            it.copy(
                instantPhotoUri = uri ?: it.recentThumbnailUri,
                isInstantPhotoVisible = true
            )
        }
    }

    fun dismissInstantPhoto() {
        _state.update {
            it.copy(
                isInstantPhotoVisible = false,
                instantPhotoUri = null
            )
        }
    }

    private fun advanceFrameCounter() {
        _state.update {
            val nextFrame = if (it.currentFrame >= it.totalFrames) 1 else it.currentFrame + 1
            it.copy(currentFrame = nextFrame)
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        diptychManager.reset()
        cameraController.release()
        try {
            captureGlExecutor.postAndWait {
                capturePipeline?.release()
                captureTextureCache.releaseAll()
            }
            captureGlExecutor.quitSafely()
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up capture GL pipeline on ViewModel clear", e)
        }
    }
}
