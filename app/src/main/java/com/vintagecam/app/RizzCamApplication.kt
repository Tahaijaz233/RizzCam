package com.vintagecam.app

import android.app.Application
import android.util.Log

/**
 * Main Application class for RizzCam Retro Analog Photography Engine.
 */
class RizzCamApplication : Application() {
    companion object {
        private const val TAG = "RizzCamApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RizzCam Engine initialized: GL ES 3.0 pipeline, CameraX, and skeuomorphic UI ready")
    }
}
