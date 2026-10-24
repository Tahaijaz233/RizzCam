package com.vintagecam.feature.viewfinder.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vintagecam.core.camera.CameraController
import com.vintagecam.feature.viewfinder.model.FramingGeometryHelper
import java.io.IOException
import kotlin.math.roundToInt
import com.vintagecam.core.camera.capture.HighResCaptureManager
import com.vintagecam.core.camera.diptych.DiptychResult
import com.vintagecam.core.camera.diptych.HalfFrameDiptychManager
import com.vintagecam.core.camera.export.MediaStoreExporter
import com.vintagecam.core.camera.model.FlashMode
import com.vintagecam.core.camera.model.FocusMeteringState
import com.vintagecam.core.camera.model.LensFacing
import com.vintagecam.core.camera.timestamp.RetroTimestampRenderer
import com.vintagecam.core.camera.timestamp.TimestampFormat
import com.vintagecam.core.camera.timestamp.TimestampStyle
import com.vintagecam.core.camera.video.RecordingState
import com.vintagecam.core.camera.video.RetroVideoFrameRateMode
import com.vintagecam.core.camera.video.RetroVideoRecorder
import com.vintagecam.feature.viewfinder.model.CaptureMode
import android.opengl.EGLSurface
import com.vintagecam.core.gl.VintageGlPipeline
import com.vintagecam.core.gl.egl.EglCore
import com.vintagecam.core.gl.egl.GlThreadExecutor
import com.vintagecam.core.gl.texture.TextureCacheManager
import com.vintagecam.domain.presets.model.AspectRatioProfile
import com.vintagecam.domain.presets.model.CameraPreset
import com.vintagecam.domain.presets.repository.PresetRepository
import com.vintagecam.domain.presets.repository.PresetRepositoryImpl
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.model.CameraModel
import com.vintagecam.feature.viewfinder.model.DazzCameraCatalog
import com.vintagecam.feature.viewfinder.model.DazzCameraPresetCatalog
import com.vintagecam.feature.viewfinder.model.DiptychCaptureStep
import com.vintagecam.feature.viewfinder.model.OpticalAccessory
import com.vintagecam.feature.viewfinder.model.ViewfinderState
import com.vintagecam.feature.viewfinder.ui.components.LensFocalLength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
 * - Manages physical lens focal length switching (18mm to 135mm framing zoom).
 * - Controls manual exposure compensation (-2.0 EV to +2.0 EV).
 * - Handles high-resolution shutter release, offscreen 6-pass GL rendering, optical framing crop, and MediaStore export.
 */
class ViewfinderViewModel @JvmOverloads constructor(
    application: Application,
    private val presetRepository: PresetRepository = PresetRepositoryImpl(application.applicationContext),
    val cameraController: CameraController = CameraController(application.applicationContext),
    val haptics: RetroHapticsEngine = RetroHapticsEngine(application.applicationContext)
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ViewfinderViewModel"

        fun Factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ViewfinderViewModel(application) as T
                }
            }
    }

    private val mediaStoreExporter = MediaStoreExporter(application.applicationContext)
    private val diptychManager = HalfFrameDiptychManager()
    val retroVideoRecorder = RetroVideoRecorder()

    private val captureGlExecutor = GlThreadExecutor("CaptureGlThread")
    private var captureEglCore: EglCore? = null
    private var capturePbufferSurface: EGLSurface? = null
    private var capturePipeline: VintageGlPipeline? = null
    private val captureTextureCache = TextureCacheManager()

    private val _state = MutableStateFlow(ViewfinderState())
    val state: StateFlow<ViewfinderState> = _state.asStateFlow()

    private val _lastCapturedPhotoUri = MutableStateFlow<Uri?>(null)
    val lastCapturedPhotoUri: StateFlow<Uri?> = _lastCapturedPhotoUri.asStateFlow()

    private var activeLifecycleOwner: LifecycleOwner? = null
    private var activeSurfaceProvider: Preview.SurfaceProvider? = null
    private var timerJob: Job? = null
    private val prefs = application.getSharedPreferences("rizzcam_prefs", Context.MODE_PRIVATE)

    init {
        val savedDateStamp = prefs.getBoolean("date_stamp_enabled", false)
        val savedStyleName = prefs.getString("date_stamp_style", TimestampStyle.AUTO.name) ?: TimestampStyle.AUTO.name
        val savedFormatName = prefs.getString("date_stamp_format", TimestampFormat.YEAR_MONTH_DAY.name) ?: TimestampFormat.YEAR_MONTH_DAY.name
        val savedGrid = prefs.getBoolean("grid_enabled", true)
        val savedSound = prefs.getBoolean("sound_enabled", true)
        val savedHaptics = prefs.getBoolean("haptics_enabled", true)
        val savedSaveOriginal = prefs.getBoolean("save_original_enabled", false)

        val resolvedStyle = try { TimestampStyle.valueOf(savedStyleName) } catch (_: Exception) { TimestampStyle.AUTO }
        val resolvedFormat = try { TimestampFormat.valueOf(savedFormatName) } catch (_: Exception) { TimestampFormat.YEAR_MONTH_DAY }

        _state.update {
            it.copy(
                dateStampEnabled = savedDateStamp,
                dateStampStyle = resolvedStyle,
                dateStampFormat = resolvedFormat,
                gridEnabled = savedGrid,
                soundEnabled = savedSound,
                hapticsEnabled = savedHaptics,
                saveOriginalEnabled = savedSaveOriginal
            )
        }
        loadPresets()
        observeCameraState()
        observeVideoRecorderState()
    }

    private fun observeVideoRecorderState() {
        viewModelScope.launch {
            retroVideoRecorder.recordingState.collect { recState ->
                _state.update { it.copy(isRecordingVideo = recState == RecordingState.RECORDING) }
            }
        }
        viewModelScope.launch {
            retroVideoRecorder.jitterState.collect { jitter ->
                _state.update { it.copy(recordingDurationMs = jitter.elapsedDurationMs) }
            }
        }
    }

    private fun ensureCapturePipelineInitialized() {
        captureGlExecutor.postAndWait {
            if (captureEglCore == null || capturePbufferSurface == null) {
                val eglCore = EglCore(flags = EglCore.FLAG_TRY_GLES3)
                val pbuffer = eglCore.createOffscreenSurface(1, 1)
                eglCore.makeCurrent(pbuffer)
                captureEglCore = eglCore
                capturePbufferSurface = pbuffer
            } else {
                capturePbufferSurface?.let { surface ->
                    captureEglCore?.makeCurrent(surface)
                }
            }
            if (capturePipeline == null) {
                capturePipeline = VintageGlPipeline.createFromAssets(getApplication<Application>().assets)
            }
        }
    }

    private fun loadPresets() {
        viewModelScope.launch {
            try {
                val presets = presetRepository.getAllPresets()
                val currentCamera = _state.value.activeCameraModel
                val initialPreset = DazzCameraPresetCatalog.getPresetForCamera(currentCamera, presets)

                _state.update {
                    it.copy(
                        presets = presets,
                        activePreset = initialPreset
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
     * Selects a camera model from the Dazz catalog, updating active preset, default focal length,
     * and closing open sheets.
     */
    fun selectCameraModel(model: CameraModel) {
        haptics.cameraSwitch()
        val matchingPreset = DazzCameraPresetCatalog.getPresetForCamera(model, _state.value.presets)

        if (model.aspectRatio != AspectRatioProfile.HALF_FRAME_DIPTYCH) {
            diptychManager.cancelPendingFrame()
        }

        val targetMode = if (model.isVideo) CaptureMode.VIDEO else CaptureMode.PHOTO
        if (model.isVideo) {
            cameraController.attachVideoCapture(retroVideoRecorder.videoCapture)
        } else {
            cameraController.detachVideoCapture()
        }

        _state.update { current ->
            val resetDiptych = if (model.aspectRatio != AspectRatioProfile.HALF_FRAME_DIPTYCH) {
                DiptychCaptureStep.IDLE
            } else {
                current.diptychStep
            }
            current.copy(
                activeCameraModel = model,
                activePreset = matchingPreset,
                captureMode = targetMode,
                focalLengthMm = model.defaultFocalLengthMm,
                diptychStep = resetDiptych,
                firstDiptychUri = if (resetDiptych == DiptychCaptureStep.IDLE) null else current.firstDiptychUri,
                isPresetDrawerOpen = false,
                isFullCatalogOpen = false,
                isSampleDialogOpen = false
            )
        }
    }

    /**
     * Sets active capture mode (PHOTO or VIDEO).
     */
    fun setCaptureMode(mode: CaptureMode) {
        if (_state.value.captureMode == mode) return
        haptics.cameraSwitch()
        _state.update { it.copy(captureMode = mode) }
        if (mode == CaptureMode.VIDEO) {
            cameraController.attachVideoCapture(retroVideoRecorder.videoCapture)
        } else {
            cameraController.detachVideoCapture()
        }
    }

    /**
     * Toggles between PHOTO and VIDEO capture modes.
     */
    fun toggleCaptureMode() {
        val nextMode = if (_state.value.captureMode == CaptureMode.PHOTO) CaptureMode.VIDEO else CaptureMode.PHOTO
        setCaptureMode(nextMode)
    }

    /**
     * Initiates retro video recording to Movies/RizzCam.
     */
    fun startVideoRecording() {
        if (_state.value.isRecordingVideo) return
        haptics.shutterClick()
        val context = getApplication<Application>().applicationContext
        val frameRateMode = when (_state.value.activeCameraModel.id) {
            "c_8mm", "v_classic" -> RetroVideoFrameRateMode.SUPER_8
            "vhs", "dcr" -> RetroVideoFrameRateMode.VHS_CAMCORDER
            else -> RetroVideoFrameRateMode.CINEMA_35MM
        }
        retroVideoRecorder.setFrameRateMode(frameRateMode)
        retroVideoRecorder.startRecording(
            context = context,
            audioEnabled = true,
            onError = { error ->
                Log.e(TAG, "Video recording error", error)
                _state.update { it.copy(errorMessage = "Recording error: ${error.message}") }
            },
            onSaved = { uri ->
                _state.update { it.copy(recentThumbnailUri = uri) }
                _lastCapturedPhotoUri.value = uri
            }
        )
    }

    /**
     * Stops active video recording.
     */
    fun stopVideoRecording() {
        if (!_state.value.isRecordingVideo) return
        haptics.shutterClick()
        retroVideoRecorder.stopRecording()
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

    /**
     * Updates optical focal length millimeter (13mm to 135mm).
     * In accordance with physical vintage framing mechanics, hardware sensor zoom stays 1.0f.
     */
    fun setFocalLengthMm(mm: Int) {
        val clamped = mm.coerceIn(13, 135)
        if (_state.value.focalLengthMm != clamped) {
            haptics.tick()
            _state.update { it.copy(focalLengthMm = clamped) }
        }
    }

    /**
     * Selects an optical accessory filter (Fisheye, ND, Color Gel, Prism, Star).
     */
    fun selectAccessory(accessory: OpticalAccessory) {
        haptics.tick()
        _state.update { it.copy(activeAccessory = accessory) }
    }

    fun openPresetDrawer() {
        haptics.tick()
        _state.update { it.copy(isPresetDrawerOpen = true) }
    }

    fun closePresetDrawer() {
        _state.update { it.copy(isPresetDrawerOpen = false) }
    }

    fun togglePresetDrawer() {
        haptics.tick()
        _state.update { it.copy(isPresetDrawerOpen = !it.isPresetDrawerOpen) }
    }

    fun openFullCatalog() {
        haptics.tick()
        _state.update { it.copy(isFullCatalogOpen = true, isPresetDrawerOpen = false) }
    }

    fun closeFullCatalog() {
        _state.update { it.copy(isFullCatalogOpen = false) }
    }

    fun openSettings() {
        haptics.tick()
        _state.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _state.update { it.copy(isSettingsOpen = false) }
    }

    fun openSampleDialog() {
        haptics.tick()
        _state.update { it.copy(isSampleDialogOpen = true) }
    }

    fun closeSampleDialog() {
        _state.update { it.copy(isSampleDialogOpen = false) }
    }

    fun toggleFocalLengthPicker() {
        haptics.tick()
        _state.update {
            it.copy(
                isFocalLengthPickerOpen = !it.isFocalLengthPickerOpen,
                isExposurePickerOpen = false
            )
        }
    }

    fun closeFocalLengthPicker() {
        _state.update { it.copy(isFocalLengthPickerOpen = false) }
    }

    fun toggleExposurePicker() {
        haptics.tick()
        _state.update {
            it.copy(
                isExposurePickerOpen = !it.isExposurePickerOpen,
                isFocalLengthPickerOpen = false
            )
        }
    }

    fun closeExposurePicker() {
        _state.update { it.copy(isExposurePickerOpen = false) }
    }

    /**
     * Cycles camera aspect ratio between 3:2, 4:3, 1:1, 6:7, 9:16 (Story), 16:9.
     */
    fun cycleAspectRatio() {
        haptics.tick()
        val currentAspect = _state.value.activeCameraModel.aspectRatio
        val nextAspect = when (currentAspect) {
            AspectRatioProfile.RATIO_3_2 -> AspectRatioProfile.RATIO_4_3
            AspectRatioProfile.RATIO_4_3 -> AspectRatioProfile.RATIO_1_1
            AspectRatioProfile.RATIO_1_1 -> AspectRatioProfile.RATIO_6_7
            AspectRatioProfile.RATIO_6_7 -> AspectRatioProfile.RATIO_9_16
            AspectRatioProfile.RATIO_9_16 -> AspectRatioProfile.RATIO_16_9
            AspectRatioProfile.RATIO_16_9 -> AspectRatioProfile.RATIO_3_2
            AspectRatioProfile.HALF_FRAME_DIPTYCH -> AspectRatioProfile.RATIO_4_3
        }
        setAspectRatio(nextAspect)
    }

    /**
     * Sets the active aspect ratio for both camera model and active filter preset.
     */
    fun setAspectRatio(aspectRatio: AspectRatioProfile) {
        _state.update { current ->
            current.copy(
                activeCameraModel = current.activeCameraModel.copy(aspectRatio = aspectRatio),
                activePreset = current.activePreset?.copy(aspectRatio = aspectRatio)
            )
        }
    }

    /**
     * Toggles double exposure mode.
     */
    fun toggleDoubleExposure() {
        haptics.tick()
        _state.update { it.copy(isDoubleExposureEnabled = !it.isDoubleExposureEnabled) }
    }

    /**
     * Cycles self-timer seconds: 0s -> 3s -> 10s -> 0s.
     */
    fun toggleSelfTimer() {
        haptics.tick()
        val nextSeconds = when (_state.value.selfTimerSeconds) {
            0 -> 3
            3 -> 10
            else -> 0
        }
        _state.update { it.copy(selfTimerSeconds = nextSeconds) }
    }

    /**
     * Cycles EV exposure compensation through discrete stops: -2 -> -1 -> 0 -> +1 -> +2.
     */
    fun cycleEvCompensation() {
        haptics.tick()
        val currentEv = _state.value.evCompensation
        val nextEv = when {
            currentEv <= -1.5f -> -1.0f
            currentEv <= -0.5f -> 0.0f
            currentEv <= 0.5f -> 1.0f
            currentEv <= 1.5f -> 2.0f
            else -> -2.0f
        }
        setEvCompensation(nextEv)
    }

    fun setGridEnabled(enabled: Boolean) {
        haptics.tick()
        prefs.edit().putBoolean("grid_enabled", enabled).apply()
        _state.update { it.copy(gridEnabled = enabled) }
    }

    fun setDateStampEnabled(enabled: Boolean) {
        haptics.tick()
        prefs.edit().putBoolean("date_stamp_enabled", enabled).apply()
        _state.update { it.copy(dateStampEnabled = enabled) }
    }

    fun setDateStampStyle(style: TimestampStyle) {
        haptics.tick()
        prefs.edit().putString("date_stamp_style", style.name).apply()
        _state.update { it.copy(dateStampStyle = style) }
    }

    fun setDateStampFormat(format: TimestampFormat) {
        haptics.tick()
        prefs.edit().putString("date_stamp_format", format.name).apply()
        _state.update { it.copy(dateStampFormat = format) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        haptics.tick()
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        _state.update { it.copy(soundEnabled = enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        haptics.tick()
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
        _state.update { it.copy(hapticsEnabled = enabled) }
    }

    fun setSaveOriginalEnabled(enabled: Boolean) {
        haptics.tick()
        prefs.edit().putBoolean("save_original_enabled", enabled).apply()
        _state.update { it.copy(saveOriginalEnabled = enabled) }
    }

    fun setSelfTimerSeconds(seconds: Int) {
        haptics.tick()
        _state.update { it.copy(selfTimerSeconds = seconds) }
    }

    /**
     * Changes optical focal length simulation and sets CameraX zoom ratio.
     */
    fun selectLens(lens: LensFocalLength) {
        val mm = when (lens) {
            LensFocalLength.FISHEYE -> 13
            LensFocalLength.LENS_28MM -> 28
            LensFocalLength.LENS_35MM -> 35
            LensFocalLength.LENS_50MM -> 50
        }
        _state.update { it.copy(selectedLens = lens, focalLengthMm = mm) }
    }

    /**
     * Adjusts manual exposure compensation in EV stops (-2.0 EV to +2.0 EV in 0.1 EV resolution).
     */
    fun setEvCompensation(ev: Float) {
        val roundedEv = ((ev * 10f).roundToInt() / 10f).coerceIn(-2.0f, 2.0f)
        _state.update { it.copy(evCompensation = roundedEv) }
        cameraController.setExposureEv(roundedEv)
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
     * In accordance with physical vintage framing mechanics, hardware sensor zoom stays 1.0f.
     * Zooming dynamically adjusts physical optical framing focal length (13mm to 135mm).
     */
    fun onZoomGesture(zoomDelta: Float) {
        val currentMm = _state.value.focalLengthMm
        val newMm = (currentMm * zoomDelta).roundToInt().coerceIn(13, 135)
        if (newMm != currentMm) {
            setFocalLengthMm(newMm)
        }
    }

    /**
     * Primary shutter actuator button entry point that coordinates self-timer countdown
     * before firing capture.
     */
    fun onShutterPressed(onPhotoCaptured: ((Uri?) -> Unit)? = null) {
        if (_state.value.captureMode == CaptureMode.VIDEO) {
            if (_state.value.isRecordingVideo) {
                stopVideoRecording()
            } else {
                startVideoRecording()
            }
            return
        }

        if (_state.value.isCapturing) return

        val timerSec = _state.value.selfTimerSeconds
        if (timerSec > 0) {
            if (_state.value.isSelfTimerCountingDown) {
                // Cancel active countdown if tapped again
                timerJob?.cancel()
                _state.update { it.copy(isSelfTimerCountingDown = false, countdownRemaining = 0) }
                return
            }
            timerJob = viewModelScope.launch {
                _state.update { it.copy(isSelfTimerCountingDown = true, countdownRemaining = timerSec) }
                for (sec in timerSec downTo 1) {
                    _state.update { it.copy(countdownRemaining = sec) }
                    haptics.tick()
                    delay(1000)
                }
                _state.update { it.copy(isSelfTimerCountingDown = false, countdownRemaining = 0) }
                takePhoto(onPhotoCaptured)
            }
        } else {
            takePhoto(onPhotoCaptured)
        }
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

        val croppedBitmap = cropToFramingGeometry(
            bitmap = captureResult.bitmap,
            aspectRatio = activePreset.aspectRatio,
            focalLengthMm = _state.value.focalLengthMm
        )
        if (croppedBitmap != captureResult.bitmap && !captureResult.bitmap.isRecycled) {
            captureResult.bitmap.recycle()
        }

        when (val diptychResult = diptychManager.processFrame(croppedBitmap)) {
            is DiptychResult.FirstFrameStored -> {
                haptics.diptychAdvance()
                _state.update {
                    it.copy(
                        diptychStep = DiptychCaptureStep.FIRST_FRAME_STORED
                    )
                }
                onPhotoCaptured?.invoke(null)
            }
            is DiptychResult.CompositeReady -> {
                haptics.shutterClick()
                _state.update { it.copy(diptychStep = DiptychCaptureStep.STITCHING) }

                try {
                    val shouldStamp = _state.value.dateStampEnabled || activePreset.compositing.enableDateStamp
                    val finalComposite = if (shouldStamp) {
                        RetroTimestampRenderer.applyTimestamp(
                            sourceBitmap = diptychResult.compositeBitmap,
                            style = _state.value.dateStampStyle,
                            format = _state.value.dateStampFormat,
                            isVideoPreset = false
                        )
                    } else {
                        diptychResult.compositeBitmap
                    }

                    val exportResult = mediaStoreExporter.exportPhoto(
                        bitmap = finalComposite,
                        preset = activePreset,
                        metadata = captureResult.exifMetadata
                    )

                    advanceFrameCounter()

                    _lastCapturedPhotoUri.value = exportResult.uri

                    _state.update {
                        it.copy(
                            diptychStep = DiptychCaptureStep.IDLE,
                            firstDiptychUri = null,
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

        val targetAspect = _state.value.activeCameraModel.aspectRatio
        val focalLengthMm = _state.value.focalLengthMm
        val croppedBitmap = cropToFramingGeometry(
            bitmap = captureResult.bitmap,
            aspectRatio = targetAspect,
            focalLengthMm = focalLengthMm
        )

        val shouldStamp = _state.value.dateStampEnabled || activePreset.compositing.enableDateStamp
        val finalBitmap = if (shouldStamp) {
            val isVideo = activePreset.category == com.vintagecam.domain.presets.model.PresetCategory.RETRO_VIDEO || _state.value.captureMode == CaptureMode.VIDEO
            RetroTimestampRenderer.applyTimestamp(
                sourceBitmap = croppedBitmap,
                style = _state.value.dateStampStyle,
                format = _state.value.dateStampFormat,
                isVideoPreset = isVideo
            )
        } else {
            croppedBitmap
        }

        try {
            val exportResult = mediaStoreExporter.exportPhoto(
                bitmap = finalBitmap,
                preset = activePreset,
                metadata = captureResult.exifMetadata
            )

            // Advance exposure counter (e.g., 24 -> 25)
            advanceFrameCounter()

            _lastCapturedPhotoUri.value = exportResult.uri

            _state.update {
                it.copy(
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
            if (croppedBitmap != captureResult.bitmap && !croppedBitmap.isRecycled) {
                croppedBitmap.recycle()
            }
            if (!captureResult.bitmap.isRecycled) {
                captureResult.bitmap.recycle()
            }
        }
    }

    /**
     * Crops full-sensor captured bitmap to the exact framing rectangle specified by
     * the active focal length millimeter scale and aspect ratio.
     */
    private fun cropToFramingGeometry(
        bitmap: Bitmap,
        aspectRatio: AspectRatioProfile,
        focalLengthMm: Int
    ): Bitmap {
        return FramingGeometryHelper.cropBitmapToFraming(bitmap, aspectRatio, focalLengthMm)
    }

    /**
     * Ingests an external photo from the user's gallery (via the 🖼️+ Import button),
     * applies the active vintage analog preset (3D LUT, grain, light leak, halation, frame border),
     * crops to framing geometry, exports the graded photo to MediaStore under Pictures/RizzCam,
     * advances the frame counter, and triggers the instant photo preview if applicable.
     */
    fun importAndProcessPhoto(uri: Uri) {
        val activePreset = _state.value.activePreset ?: return
        if (_state.value.isCapturing) return

        viewModelScope.launch(Dispatchers.Default) {
            _state.update { it.copy(isCapturing = true) }
            try {
                val context = getApplication<Application>().applicationContext
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Unable to open stream for URI: $uri")

                val sourceBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (sourceBitmap == null) {
                    throw IOException("Failed to decode bitmap from image URI")
                }

                ensureCapturePipelineInitialized()
                val pipeline = capturePipeline ?: throw IllegalStateException("Capture pipeline not available")
                val assets = getApplication<Application>().assets

                val (lutId, leakId, borderId) = captureGlExecutor.postWithResult {
                    val lut = captureTextureCache.getOrLoadLut3d(assets, activePreset.lut.assetPath, activePreset.lut.lutSize)
                    val leak = captureTextureCache.getOrLoadLightLeak(assets, activePreset.compositing.lightLeakPool.firstOrNull())
                    val border = captureTextureCache.getOrLoadBorderMask(assets, activePreset.compositing.borderMaskAsset)
                    Triple(lut, leak, border)
                }

                val captureManager = HighResCaptureManager(
                    context = context,
                    glThreadExecutor = captureGlExecutor,
                    pipeline = pipeline
                )

                val gradedBitmap = captureGlExecutor.postWithResult {
                    captureManager.renderGlPipelineOffscreen(
                        sourceBitmap = sourceBitmap,
                        preset = activePreset,
                        lutTextureId = lutId,
                        lightLeakTextureId = leakId,
                        borderMaskTextureId = borderId,
                        exposureEv = _state.value.evCompensation
                    )
                }

                val targetAspect = _state.value.activeCameraModel.aspectRatio
                val croppedBitmap = FramingGeometryHelper.cropBitmapToFraming(
                    bitmap = gradedBitmap,
                    aspectRatio = targetAspect,
                    focalLengthMm = _state.value.focalLengthMm
                )

                val shouldStamp = _state.value.dateStampEnabled || activePreset.compositing.enableDateStamp
                val finalBitmap = if (shouldStamp) {
                    val isVideo = activePreset.category == com.vintagecam.domain.presets.model.PresetCategory.RETRO_VIDEO
                    RetroTimestampRenderer.applyTimestamp(
                        sourceBitmap = croppedBitmap,
                        style = _state.value.dateStampStyle,
                        format = _state.value.dateStampFormat,
                        isVideoPreset = isVideo
                    )
                } else {
                    croppedBitmap
                }

                try {
                    val defaultExif = com.vintagecam.core.camera.exif.CapturedExifMetadata(
                        iso = "400",
                        fNumber = "2.0",
                        focalLength = "${_state.value.focalLengthMm}/1",
                        exposureTime = "1/60",
                        dateTimeOriginal = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                    )

                    val exportResult = mediaStoreExporter.exportPhoto(
                        bitmap = finalBitmap,
                        preset = activePreset,
                        metadata = defaultExif
                    )

                    advanceFrameCounter()
                    haptics.shutterClick()

                    _lastCapturedPhotoUri.value = exportResult.uri
                    _state.update {
                        it.copy(
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
                } finally {
                    if (croppedBitmap != gradedBitmap && !croppedBitmap.isRecycled) {
                        croppedBitmap.recycle()
                    }
                    if (!gradedBitmap.isRecycled) {
                        gradedBitmap.recycle()
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Import photo processing failed", t)
                _state.update { it.copy(errorMessage = "Photo import failed: ${t.message}") }
            } finally {
                _state.update { it.copy(isCapturing = false) }
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
                capturePipeline = null
                captureTextureCache.releaseAll()
                capturePbufferSurface?.let { surface ->
                    captureEglCore?.releaseSurface(surface)
                    capturePbufferSurface = null
                }
                captureEglCore?.release()
                captureEglCore = null
            }
            captureGlExecutor.quitSafely()
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up capture GL pipeline on ViewModel clear", e)
        }
    }
}
