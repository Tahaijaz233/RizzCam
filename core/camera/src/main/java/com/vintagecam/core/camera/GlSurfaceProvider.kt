package com.vintagecam.core.camera

import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import com.vintagecam.core.gl.egl.GlThreadExecutor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX [Preview.SurfaceProvider] implementation that routes camera preview frames
 * directly into an OpenGL ES external OES texture ([android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES]).
 *
 * Architecture & Thread Safety:
 * - Decouples CameraX background worker threads from the dedicated GL render thread ([GlThreadExecutor]).
 * - Frame availability listener is dispatched directly onto [GlThreadExecutor.handler] to eliminate
 *   inter-thread synchronization overhead during the 60 FPS preview loop.
 * - Allocates zero heap objects inside [SurfaceTexture.OnFrameAvailableListener.onFrameAvailable] to ensure
 *   zero GC pauses and strictly adhere to sub-16.6ms render deadlines.
 * - Implements deterministic teardown via [release] and [SurfaceRequest.provideSurface] completion listener.
 *
 * @param oesTextureId Pre-allocated OpenGL texture handle generated with target GL_TEXTURE_EXTERNAL_OES.
 * @param glThreadExecutor Single-threaded executor with display priority running the EGL context.
 * @param onFrameReady Callback invoked on the GL thread whenever a new camera frame is ready,
 *                      passing the 4x4 UV transformation matrix provided by [SurfaceTexture.getTransformMatrix].
 */
class GlSurfaceProvider(
    private val oesTextureId: Int,
    private val glThreadExecutor: GlThreadExecutor,
    private val onFrameReady: (transformMatrix: FloatArray) -> Unit
) : Preview.SurfaceProvider {

    companion object {
        private const val TAG = "GlSurfaceProvider"
    }

    // Preallocated 4x4 matrix buffer to avoid per-frame allocations during frame delivery
    private val transformMatrix = FloatArray(16)

    @Volatile
    private var activeSurfaceTexture: SurfaceTexture? = null

    @Volatile
    private var activeSurface: Surface? = null

    private val isReleased = AtomicBoolean(false)
    private val lock = Any()

    override fun onSurfaceRequested(request: SurfaceRequest) {
        if (isReleased.get()) {
            Log.w(TAG, "onSurfaceRequested received after GlSurfaceProvider was released. Will cancel request.")
            request.willNotProvideSurface()
            return
        }

        val resolution = request.resolution
        Log.i(TAG, "Surface requested with resolution: ${resolution.width}x${resolution.height}")

        // Ensure any previous surface and surfaceTexture are cleanly released on GL thread
        cleanupActiveSurface()

        synchronized(lock) {
            if (isReleased.get()) {
                request.willNotProvideSurface()
                return
            }

            try {
                // Instantiate SurfaceTexture directly on the GL thread where EGL context is active
                val surfaceTexture = glThreadExecutor.postWithResult {
                    SurfaceTexture(oesTextureId).apply {
                        setDefaultBufferSize(resolution.width, resolution.height)
                    }
                }

                // Register frame available listener with GL thread handler
                surfaceTexture.setOnFrameAvailableListener({ st ->
                    if (isReleased.get()) return@setOnFrameAvailableListener

                    try {
                        // Must be called on the thread with active EGL context (GL thread)
                        st.updateTexImage()
                        st.getTransformMatrix(transformMatrix)
                        onFrameReady(transformMatrix)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in onFrameAvailable while updating OES texture: ${e.message}")
                    }
                }, glThreadExecutor.handler)

                val surface = Surface(surfaceTexture)
                activeSurfaceTexture = surfaceTexture
                activeSurface = surface

                // Provide surface to CameraX with release listener
                request.provideSurface(surface, glThreadExecutor) { result ->
                    Log.d(TAG, "CameraX SurfaceRequest result: ${result.resultCode}")
                    synchronized(lock) {
                        if (activeSurface === surface) {
                            activeSurface = null
                        }
                        if (activeSurfaceTexture === surfaceTexture) {
                            activeSurfaceTexture = null
                        }
                    }
                    surface.release()
                    surfaceTexture.release()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to create and provide Surface for CameraX request", e)
                request.willNotProvideSurface()
            }
        }
    }

    private fun cleanupActiveSurface() {
        synchronized(lock) {
            val surface = activeSurface
            val surfaceTexture = activeSurfaceTexture
            activeSurface = null
            activeSurfaceTexture = null

            if (surface != null || surfaceTexture != null) {
                val posted = glThreadExecutor.handler.post {
                    try {
                        surface?.release()
                        surfaceTexture?.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error during cleanup of previous surface: ${e.message}")
                    }
                }
                if (!posted) {
                    try {
                        surface?.release()
                        surfaceTexture?.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Direct fallback cleanup error: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Deterministically cleans up all allocated [Surface] and [SurfaceTexture] handles
     * and prevents subsequent frame processing.
     */
    fun release() {
        if (!isReleased.compareAndSet(false, true)) {
            return
        }

        Log.i(TAG, "Releasing GlSurfaceProvider")
        cleanupActiveSurface()
    }
}
