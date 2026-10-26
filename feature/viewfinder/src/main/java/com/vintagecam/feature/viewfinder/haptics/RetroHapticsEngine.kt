package com.vintagecam.feature.viewfinder.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Skeuomorphic mechanical haptics engine for vintage analog camera interactions.
 *
 * Utilizes the device's linear resonant actuator (LRA) or haptic motor to synthesize
 * physical mechanical sensations:
 * - [tick]: Light rotary detent click for knurled dial rotation and 1/3-stop EV steps.
 * - [shutterClick]: Heavy two-stage mechanical clunk simulating mirror trip and shutter curtain release.
 * - [diptychAdvance]: Double pulse simulating manual film advance lever ratchet when storing half-frame shots.
 * - [cameraSwitch]: Firm tactile lock when rotating the turret to switch camera bodies.
 * - [focusLocked]: Soft confirmation pulse upon auto-focus lock.
 */
class RetroHapticsEngine(private val context: Context) {

    companion object {
        private const val TAG = "RetroHapticsEngine"
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Crisp, subtle tactile click triggered on each rotational click of the knurled exposure dial
     * or lens wheel step.
     */
    fun tick() {
        vibrateSafely {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(8L, 50))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10L)
            }
        }
    }

    /**
     * Heavy two-stage mechanical clunk simulating physical mirror flip and focal plane shutter curtain.
     * Stage 1: Spring release latch trip (15ms @ amp 160).
     * Intermission: 30ms curtain travel.
     * Stage 2: Curtain brake impact (25ms @ amp 255).
     */
    fun shutterClick() {
        vibrateSafely {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 16, 32, 28)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(55L)
            }
        }
    }

    /**
     * Dual ratchet pulse simulating manual film advance lever indexing to the next half-frame.
     */
    fun diptychAdvance() {
        vibrateSafely {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 18, 55, 24)
                val amplitudes = intArrayOf(0, 130, 0, 170)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 18, 55, 24), -1)
            }
        }
    }

    /**
     * Firm mechanical click when switching between analog camera presets or lens turrets.
     */
    fun cameraSwitch() {
        vibrateSafely {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(22L, 200))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25L)
            }
        }
    }

    /**
     * Soft haptic pulse confirming AF/AE lock.
     */
    fun focusLocked() {
        vibrateSafely {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(12L, 90))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15L)
            }
        }
    }

    /**
     * Crisp double click triggered when slider crosses 0 (neutral white balance).
     */
    fun doubleClick() {
        vibrateSafely {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 10, 40, 12)
                val amplitudes = intArrayOf(0, 120, 0, 180)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 10, 40, 12), -1)
            }
        }
    }

    /**
     * Standard iOS click on panel open / close or button actuation.
     */
    fun click() {
        vibrateSafely {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(15L, 160))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15L)
            }
        }
    }

    /**
     * Heavy click on long-press reset to 0 neutral white balance.
     */
    fun heavyClick() {
        vibrateSafely {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30L, 255))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30L)
            }
        }
    }

    private inline fun vibrateSafely(action: () -> Unit) {
        try {
            if (vibrator?.hasVibrator() == true) {
                action()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Haptic actuation failed: ${t.message}")
        }
    }
}
