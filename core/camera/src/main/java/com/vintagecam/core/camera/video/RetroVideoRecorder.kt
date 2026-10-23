package com.vintagecam.core.camera.video

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

/**
 * Retro video recording frame rate profiles.
 */
enum class RetroVideoFrameRateMode(
    val targetFps: Int,
    val frameDurationMs: Long,
    val displayName: String
) {
    SUPER_8(
        targetFps = 18,
        frameDurationMs = 55L,
        displayName = "Super 8 (18 FPS)"
    ),
    CINEMA_35MM(
        targetFps = 24,
        frameDurationMs = 41L,
        displayName = "Cinema (24 FPS)"
    ),
    VHS_CAMCORDER(
        targetFps = 30,
        frameDurationMs = 33L,
        displayName = "VHS (30 FPS)"
    )
}

/**
 * Recording state lifecycle.
 */
enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    FINALIZING
}

/**
 * Real-time optical gate flutter and scanline jitter telemetry.
 */
data class RetroJitterState(
    val gateFlutterX: Float = 0.0f,
    val gateFlutterY: Float = 0.0f,
    val scanlineJitter: Float = 0.0f,
    val exposureFlicker: Float = 0.0f,
    val elapsedDurationMs: Long = 0L
)

/**
 * Retro Video Simulation Engine.
 *
 * Encapsulates:
 * - CameraX [VideoCapture] and [Recorder] with high-bitrate FHD recording to `Movies/RizzCam`.
 * - Retro frame rate simulation (18 FPS Super 8, 24 FPS Cinema, 30 FPS VHS).
 * - Real-time mechanical film gate flutter simulation (claw jitter & vertical gate weave).
 * - Analog magnetic tape scanline jitter & head-switch tracking flutter.
 * - Reactive [StateFlow] lifecycle states and zero-leak thread management.
 */
class RetroVideoRecorder(
    val recorder: Recorder = Recorder.Builder()
        .setQualitySelector(
            QualitySelector.from(
                Quality.FHD,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )
        )
        .build()
) {
    companion object {
        private const val TAG = "RetroVideoRecorder"
        private const val OUTPUT_DIRECTORY_NAME = "Movies/RizzCam"
    }

    val videoCapture: VideoCapture<Recorder> = VideoCapture.withOutput(recorder)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var simulationJob: Job? = null

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _frameRateMode = MutableStateFlow(RetroVideoFrameRateMode.SUPER_8)
    val frameRateMode: StateFlow<RetroVideoFrameRateMode> = _frameRateMode.asStateFlow()

    private val _jitterState = MutableStateFlow(RetroJitterState())
    val jitterState: StateFlow<RetroJitterState> = _jitterState.asStateFlow()

    private var activeRecording: Recording? = null
    private val rng = Random()

    fun setFrameRateMode(mode: RetroVideoFrameRateMode) {
        _frameRateMode.value = mode
    }

    /**
     * Initiates video recording directed to `Movies/RizzCam` in MediaStore (or public directory on API <= 28).
     */
    @SuppressLint("MissingPermission")
    fun startRecording(
        context: Context,
        audioEnabled: Boolean = true,
        onError: ((Throwable) -> Unit)? = null,
        onSaved: ((Uri) -> Unit)? = null
    ) {
        if (_recordingState.value != RecordingState.IDLE) {
            Log.w(TAG, "Recording already active or finalizing, ignoring start request")
            return
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "RIZZCAM_${_frameRateMode.value.name}_$timestamp.mp4"

        var pending: PendingRecording = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, OUTPUT_DIRECTORY_NAME)
            }
            val options = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(contentValues).build()
            recorder.prepareRecording(context, options)
        } else {
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val targetDir = File(moviesDir, "RizzCam").apply { mkdirs() }
            val targetFile = File(targetDir, fileName)
            val options = FileOutputOptions.Builder(targetFile).build()
            recorder.prepareRecording(context, options)
        }

        val canRecordAudio = audioEnabled &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

        if (canRecordAudio) {
            pending = pending.withAudioEnabled()
        }

        _recordingState.value = RecordingState.RECORDING
        startSimulationTicker()

        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event: VideoRecordEvent ->
            handleRecordEvent(event, onError, onSaved)
        }
    }

    private fun handleRecordEvent(
        event: VideoRecordEvent,
        onError: ((Throwable) -> Unit)?,
        onSaved: ((Uri) -> Unit)?
    ) {
        when (event) {
            is VideoRecordEvent.Start -> {
                Log.d(TAG, "Recording started successfully")
                _recordingState.value = RecordingState.RECORDING
            }
            is VideoRecordEvent.Pause -> {
                Log.d(TAG, "Recording paused")
                _recordingState.value = RecordingState.PAUSED
            }
            is VideoRecordEvent.Resume -> {
                Log.d(TAG, "Recording resumed")
                _recordingState.value = RecordingState.RECORDING
            }
            is VideoRecordEvent.Finalize -> {
                stopSimulationTicker()
                _recordingState.value = RecordingState.IDLE
                activeRecording = null

                if (event.hasError()) {
                    Log.e(TAG, "Video recording finalized with error: ${event.error}", event.cause)
                    onError?.invoke(event.cause ?: RuntimeException("Recording error: ${event.error}"))
                } else {
                    val uri = event.outputResults.outputUri
                    Log.d(TAG, "Video recording finalized successfully: $uri")
                    onSaved?.invoke(uri)
                }
            }
            is VideoRecordEvent.Status -> {
                val stats = event.recordingStats
                _jitterState.value = _jitterState.value.copy(
                    elapsedDurationMs = stats.recordedDurationNanos / 1_000_000L
                )
            }
        }
    }

    fun pauseRecording() {
        if (_recordingState.value == RecordingState.RECORDING) {
            activeRecording?.pause()
        }
    }

    fun resumeRecording() {
        if (_recordingState.value == RecordingState.PAUSED) {
            activeRecording?.resume()
        }
    }

    fun stopRecording() {
        if (_recordingState.value == RecordingState.RECORDING || _recordingState.value == RecordingState.PAUSED) {
            _recordingState.value = RecordingState.FINALIZING
            activeRecording?.stop()
        }
    }

    /**
     * Runs real-time film gate flutter and scanline jitter simulation matching the selected mode.
     */
    private fun startSimulationTicker() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            var startNs = System.nanoTime()

            while (isActive) {
                if (_recordingState.value == RecordingState.PAUSED) {
                    delay(50L)
                    continue
                }

                val mode = _frameRateMode.value

                val flutterX: Float
                val flutterY: Float
                val scanline: Float
                val flicker: Float

                when (mode) {
                    RetroVideoFrameRateMode.SUPER_8 -> {
                        // Mechanical Super 8 claw gate play: periodic frame jumps and gate weave
                        val clawJump = if (rng.nextFloat() < 0.12f) (rng.nextFloat() - 0.5f) * 0.014f else 0.0f
                        flutterX = (rng.nextFloat() - 0.5f) * 0.003f
                        flutterY = ((rng.nextFloat() - 0.5f) * 0.005f) + clawJump
                        scanline = 0.0f
                        // Rotary disc shutter mechanical exposure flicker (+- 4%)
                        flicker = (rng.nextFloat() - 0.5f) * 0.08f
                    }
                    RetroVideoFrameRateMode.CINEMA_35MM -> {
                        // High-precision pin-registered 35mm gate: subtle natural weave
                        flutterX = (rng.nextFloat() - 0.5f) * 0.0012f
                        flutterY = (rng.nextFloat() - 0.5f) * 0.0018f
                        scanline = 0.0f
                        flicker = (rng.nextFloat() - 0.5f) * 0.02f
                    }
                    RetroVideoFrameRateMode.VHS_CAMCORDER -> {
                        // Electronic helical scan head switching noise and horizontal tracking jitter
                        val isHeadSwitchBurst = rng.nextFloat() < 0.15f
                        flutterX = 0.0f
                        flutterY = (rng.nextFloat() - 0.5f) * 0.001f
                        scanline = if (isHeadSwitchBurst) (rng.nextFloat() - 0.5f) * 0.022f else (rng.nextFloat() - 0.5f) * 0.003f
                        flicker = (rng.nextFloat() - 0.5f) * 0.03f
                    }
                }

                _jitterState.value = _jitterState.value.copy(
                    gateFlutterX = flutterX,
                    gateFlutterY = flutterY,
                    scanlineJitter = scanline,
                    exposureFlicker = flicker
                )

                delay(mode.frameDurationMs)
            }
        }
    }

    private fun stopSimulationTicker() {
        simulationJob?.cancel()
        simulationJob = null
        _jitterState.value = RetroJitterState()
    }

    fun release() {
        stopSimulationTicker()
        try {
            activeRecording?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping active recording during release", e)
        }
        activeRecording = null
        scope.cancel()
    }
}
