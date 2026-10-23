package com.vintagecam.core.gl.fbo

import android.opengl.GLES30

/**
 * Encapsulates an OpenGL ES 3.0 Framebuffer Object (FBO) backed by a 2D color texture attachment.
 *
 * Supports high-precision [GLES30.GL_RGBA16F] (half-float) attachments to prevent banding
 * artifacts across multi-pass color transformations, LUT applications, and bloom composites.
 *
 * Strict Memory Management:
 * Implements deterministic [release] to free both the GPU framebuffer handle and underlying
 * texture memory immediately.
 */
class FramebufferObject(
    val width: Int,
    val height: Int,
    val useFloat16: Boolean = true
) {
    var fboId: Int = 0
        private set

    var textureId: Int = 0
        private set

    // Reusable single-element array to avoid GC allocations during release
    private val idBuffer = IntArray(1)

    init {
        allocate()
    }

    private fun allocate() {
        val fbos = IntArray(1)
        val textures = IntArray(1)

        GLES30.glGenFramebuffers(1, fbos, 0)
        GLES30.glGenTextures(1, textures, 0)

        fboId = fbos[0]
        textureId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        val internalFormat = if (useFloat16) GLES30.GL_RGBA16F else GLES30.GL_RGBA8
        val type = if (useFloat16) GLES30.GL_HALF_FLOAT else GLES30.GL_UNSIGNED_BYTE

        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, internalFormat,
            width, height, 0, GLES30.GL_RGBA, type, null
        )

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, textureId, 0
        )

        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        check(status == GLES30.GL_FRAMEBUFFER_COMPLETE) {
            "FBO allocation failed for ${width}x${height} with status: 0x${Integer.toHexString(status)}"
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    /**
     * Binds this FBO as the active render target and sets the viewport to (0, 0, width, height).
     */
    fun bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glViewport(0, 0, width, height)
    }

    /**
     * Unbinds this FBO and restores the default framebuffer (0).
     */
    fun unbind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    /**
     * Deterministically deletes the GPU framebuffer and associated texture.
     */
    fun release() {
        if (fboId != 0) {
            idBuffer[0] = fboId
            GLES30.glDeleteFramebuffers(1, idBuffer, 0)
            fboId = 0
        }
        if (textureId != 0) {
            idBuffer[0] = textureId
            GLES30.glDeleteTextures(1, idBuffer, 0)
            textureId = 0
        }
    }
}
