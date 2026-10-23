package com.vintagecam.core.gl.egl

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.util.Log
import android.view.Surface

/**
 * Core EGL 1.4 state manager targeting OpenGL ES 3.0+.
 *
 * Provides thread-safe creation, binding, and deterministic teardown of EGL displays,
 * contexts, window surfaces (for Viewfinder display and MediaCodec encoding), and offscreen
 * pbuffer surfaces.
 *
 * Strict Memory & Resource Budget:
 * All native EGL handles are destroyed deterministically via [releaseSurface] and [release].
 * No hidden allocations or dangling GPU contexts.
 */
class EglCore(
    sharedContext: EGLContext = EGL14.EGL_NO_CONTEXT,
    flags: Int = FLAG_TRY_GLES3
) {
    companion object {
        private const val TAG = "EglCore"

        const val FLAG_RECORDABLE = 0x01
        const val FLAG_TRY_GLES3 = 0x02

        // EGL constants for OpenGL ES 3.0 and Android extensions
        private const val EGL_OPENGL_ES3_BIT_KHR = 0x00000040
        private const val EGL_RECORDABLE_ANDROID = 0x3142
    }

    var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private set
    var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private set
    var eglConfig: EGLConfig? = null
        private set

    var glVersion: Int = -1
        private set

    init {
        initEgl(sharedContext, flags)
    }

    private fun initEgl(sharedContext: EGLContext, flags: Int) {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            throw IllegalStateException("EGL already initialized")
        }

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            eglDisplay = EGL14.EGL_NO_DISPLAY
            throw RuntimeException("unable to initialize EGL14")
        }

        // Try OpenGL ES 3.0 first if requested
        if ((flags and FLAG_TRY_GLES3) != 0) {
            val config3 = getConfig(flags, 3)
            if (config3 != null) {
                val attrib3List = intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                    EGL14.EGL_NONE
                )
                val context3 = EGL14.eglCreateContext(
                    eglDisplay, config3, sharedContext,
                    attrib3List, 0
                )
                if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                    eglConfig = config3
                    eglContext = context3
                    glVersion = 3
                }
            }
        }

        // Fallback to ES 2.0 if ES 3.0 context creation failed
        if (eglContext === EGL14.EGL_NO_CONTEXT) {
            val config2 = getConfig(flags, 2) ?: throw RuntimeException("Unable to find suitable EGLConfig")
            val attrib2List = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            val context2 = EGL14.eglCreateContext(
                eglDisplay, config2, sharedContext,
                attrib2List, 0
            )
            checkEglError("eglCreateContext ES2")
            eglConfig = config2
            eglContext = context2
            glVersion = 2
        }

        // Confirm with a query
        val values = IntArray(1)
        EGL14.eglQueryContext(
            eglDisplay, eglContext,
            EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0
        )
        Log.i(TAG, "EGLContext created, client version: ${values[0]}")
    }

    private fun getConfig(flags: Int, version: Int): EGLConfig? {
        var renderableType = EGL14.EGL_OPENGL_ES2_BIT
        if (version >= 3) {
            renderableType = renderableType or EGL_OPENGL_ES3_BIT_KHR
        }

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, renderableType,
            if ((flags and FLAG_RECORDABLE) != 0) EGL_RECORDABLE_ANDROID else EGL14.EGL_NONE,
            if ((flags and FLAG_RECORDABLE) != 0) 1 else EGL14.EGL_NONE,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay, attribList, 0,
                configs, 0, configs.size,
                numConfigs, 0
            ) || numConfigs[0] <= 0
        ) {
            Log.w(TAG, "unable to find RGB8888 / $version EGLConfig")
            return null
        }
        return configs[0]
    }

    /**
     * Creates an EGL window surface from a [Surface] or [SurfaceTexture].
     */
    fun createWindowSurface(surface: Any): EGLSurface {
        if (surface !is Surface && surface !is SurfaceTexture) {
            throw IllegalArgumentException("invalid surface: $surface")
        }

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay, eglConfig, surface,
            surfaceAttribs, 0
        )
        checkEglError("eglCreateWindowSurface")
        if (eglSurface === EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("surface was null")
        }
        return eglSurface
    }

    /**
     * Creates an offscreen pbuffer surface.
     */
    fun createOffscreenSurface(width: Int, height: Int): EGLSurface {
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        val eglSurface = EGL14.eglCreatePbufferSurface(
            eglDisplay, eglConfig,
            surfaceAttribs, 0
        )
        checkEglError("eglCreatePbufferSurface")
        if (eglSurface === EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("surface was null")
        }
        return eglSurface
    }

    /**
     * Makes our context current on the calling thread with the specified draw/read surfaces.
     */
    fun makeCurrent(drawSurface: EGLSurface, readSurface: EGLSurface = drawSurface) {
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            Log.d(TAG, "makeCurrent: eglDisplay is EGL_NO_DISPLAY")
            return
        }
        if (!EGL14.eglMakeCurrent(eglDisplay, drawSurface, readSurface, eglContext)) {
            throw RuntimeException("eglMakeCurrent failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
        }
    }

    /**
     * Clears current context on the calling thread.
     */
    fun makeNothingCurrent() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
        }
    }

    /**
     * Submits rendered buffer to the display surface.
     */
    fun swapBuffers(surface: EGLSurface): Boolean {
        return EGL14.eglSwapBuffers(eglDisplay, surface)
    }

    /**
     * Sets display presentation timestamp in nanoseconds.
     */
    fun setPresentationTime(surface: EGLSurface, nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, surface, nsecs)
    }

    /**
     * Destroys an allocated EGLSurface deterministically.
     */
    fun releaseSurface(surface: EGLSurface) {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY && surface !== EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, surface)
        }
    }

    /**
     * Destroys the EGL context, display connection, and resets state.
     */
    fun release() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            makeNothingCurrent()
            if (eglContext !== EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
            }
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglConfig = null
    }

    private fun checkEglError(msg: String) {
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            throw RuntimeException("$msg: EGL error: 0x${Integer.toHexString(error)}")
        }
    }
}
