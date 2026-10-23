package com.vintagecam.core.gl.pass

import android.opengl.GLES30
import com.vintagecam.core.gl.shader.GlProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Abstract base class for all OpenGL ES 3.0 render passes.
 *
 * Encapsulates:
 * - A full-screen quad mesh backed by a native direct FloatBuffer and Hardware VAO / VBO.
 * - Zero GC allocations during [drawFullscreenQuad].
 * - Program binding and deterministic resource destruction.
 */
abstract class GlRenderPass(
    val program: GlProgram
) {
    companion object {
        // Fullscreen quad in NDC: Triangle strip with interleaved (X, Y, U, V)
        // 4 vertices * 4 floats * 4 bytes = 64 bytes total
        private val QUAD_VERTICES = floatArrayOf(
            // X,     Y,    U,    V
            -1.0f, -1.0f, 0.0f, 0.0f, // Bottom-Left
             1.0f, -1.0f, 1.0f, 0.0f, // Bottom-Right
            -1.0f,  1.0f, 0.0f, 1.0f, // Top-Left
             1.0f,  1.0f, 1.0f, 1.0f  // Top-Right
        )

        private const val FLOAT_SIZE = 4
        private const val STRIDE_BYTES = 4 * FLOAT_SIZE // 16 bytes

        val IDENTITY_MATRIX = FloatArray(16).apply {
            android.opengl.Matrix.setIdentityM(this, 0)
        }
    }

    private var vaoId: Int = 0
    private var vboId: Int = 0
    private val bufferHandles = IntArray(1)

    init {
        setupMesh()
    }

    private fun setupMesh() {
        val vertexBuffer: FloatBuffer = ByteBuffer
            .allocateDirect(QUAD_VERTICES.size * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(QUAD_VERTICES)
                position(0)
            }

        val vaos = IntArray(1)
        val vbos = IntArray(1)

        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)

        vaoId = vaos[0]
        vboId = vbos[0]

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            QUAD_VERTICES.size * FLOAT_SIZE,
            vertexBuffer,
            GLES30.GL_STATIC_DRAW
        )

        // aPosition at location = 0
        GLES30.glVertexAttribPointer(
            0, 2, GLES30.GL_FLOAT, false,
            STRIDE_BYTES, 0
        )
        GLES30.glEnableVertexAttribArray(0)

        // aTexCoord at location = 1
        GLES30.glVertexAttribPointer(
            1, 2, GLES30.GL_FLOAT, false,
            STRIDE_BYTES, 2 * FLOAT_SIZE
        )
        GLES30.glEnableVertexAttribArray(1)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindVertexArray(0)
    }

    /**
     * Binds the fullscreen quad VAO and renders the 4 vertices as a TRIANGLE_STRIP.
     * Guaranteed zero heap allocations per call.
     */
    protected fun drawFullscreenQuad() {
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    /**
     * Releases mesh VAO, VBO, and the underlying [GlProgram] shader.
     */
    open fun release() {
        if (vaoId != 0) {
            bufferHandles[0] = vaoId
            GLES30.glDeleteVertexArrays(1, bufferHandles, 0)
            vaoId = 0
        }
        if (vboId != 0) {
            bufferHandles[0] = vboId
            GLES30.glDeleteBuffers(1, bufferHandles, 0)
            vboId = 0
        }
        program.release()
    }
}
