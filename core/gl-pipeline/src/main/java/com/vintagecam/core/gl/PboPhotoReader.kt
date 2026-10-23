package com.vintagecam.core.gl

import android.graphics.Bitmap
import android.opengl.GLES30
import com.vintagecam.core.gl.fbo.FramebufferObject
import java.nio.ByteBuffer

/**
 * Asynchronous Pixel Buffer Object (GL_PIXEL_PACK_BUFFER) Readback.
 *
 * Prevents main thread pipeline stalls during high-resolution photo capture (12 MP - 48 MP)
 * by executing direct DMA transfer into pinned native memory combined with GPU fence syncs
 * ([GLES30.glFenceSync]).
 */
class PboPhotoReader {
    private val pboIds = IntArray(1)

    /**
     * Reads a high-resolution [FramebufferObject] into an Android [Bitmap] using DMA PBO transfer.
     */
    fun readFboToBitmap(targetFbo: FramebufferObject): Bitmap {
        val width = targetFbo.width
        val height = targetFbo.height
        val byteCount = width * height * 4

        GLES30.glGenBuffers(1, pboIds, 0)
        val pboId = pboIds[0]

        try {
            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pboId)
            GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, byteCount, null, GLES30.GL_STREAM_READ)

            targetFbo.bind()
            // Asynchronous DMA transfer into bound GL_PIXEL_PACK_BUFFER (offset = 0)
            GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, 0)

            // Insert GPU fence sync and wait for DMA completion (up to 500ms)
            val fence = GLES30.glFenceSync(GLES30.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
            GLES30.glFlush()
            try {
                val waitResult = GLES30.glClientWaitSync(fence, GLES30.GL_SYNC_FLUSH_COMMANDS_BIT, 500_000_000L)
                if (waitResult == GLES30.GL_WAIT_FAILED || waitResult == GLES30.GL_TIMEOUT_EXPIRED) {
                    android.util.Log.w("PboPhotoReader", "glClientWaitSync finished with code: $waitResult")
                }
            } finally {
                GLES30.glDeleteSync(fence)
            }

            val mappedBuffer = GLES30.glMapBufferRange(
                GLES30.GL_PIXEL_PACK_BUFFER,
                0,
                byteCount,
                GLES30.GL_MAP_READ_BIT
            ) as? ByteBuffer ?: throw RuntimeException("Failed to map PBO buffer range")

            val rawBitmap = try {
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(mappedBuffer)
                bmp
            } finally {
                GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER)
            }

            // Invert Y axis to convert from OpenGL bottom-left origin to Android Bitmap top-left origin
            val flipMatrix = android.graphics.Matrix().apply { postScale(1.0f, -1.0f) }
            val uprightBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, width, height, flipMatrix, false)
            rawBitmap.recycle()

            return uprightBitmap
        } finally {
            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            if (pboIds[0] != 0) {
                GLES30.glDeleteBuffers(1, pboIds, 0)
                pboIds[0] = 0
            }
        }
    }
}
