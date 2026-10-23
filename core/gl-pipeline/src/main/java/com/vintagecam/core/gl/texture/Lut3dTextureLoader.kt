package com.vintagecam.core.gl.texture

import android.content.res.AssetManager
import android.opengl.GLES30
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance 3D Texture Loader for OpenGL ES 3.0.
 *
 * Loads pre-quantized 64x64x64 RGB8 binary LUT assets (.raw3d) into direct ByteBuffers
 * and uploads them to the GPU via [GLES30.glTexImage3D] in < 2ms with zero string parsing.
 *
 * Also provides an identity LUT generator for testing and validation.
 */
object Lut3dTextureLoader {
    private const val TAG = "Lut3dTextureLoader"
    const val DEFAULT_LUT_SIZE = 64

    /**
     * Loads a .raw3d asset directly into a GPU 3D texture.
     * Total memory allocated: 786,432 bytes (64^3 * 3 bytes).
     */
    fun loadFromAssets(assetManager: AssetManager, assetPath: String, size: Int = DEFAULT_LUT_SIZE): Int {
        val expectedBytes = size * size * size * 3
        val directBuffer = ByteBuffer.allocateDirect(expectedBytes).order(ByteOrder.nativeOrder())

        assetManager.open(assetPath).use { inputStream ->
            readStreamToBuffer(inputStream, directBuffer, expectedBytes)
        }
        directBuffer.position(0)

        return upload3dTexture(directBuffer, size)
    }

    /**
     * Creates an un-graded identity 3D LUT texture where output RGB == input RGB.
     */
    fun createIdentityLut(size: Int = DEFAULT_LUT_SIZE): Int {
        val totalBytes = size * size * size * 3
        val directBuffer = ByteBuffer.allocateDirect(totalBytes).order(ByteOrder.nativeOrder())

        val step = 255.0f / (size - 1)
        for (b in 0 until size) {
            val blueByte = (b * step).toInt().toByte()
            for (g in 0 until size) {
                val greenByte = (g * step).toInt().toByte()
                for (r in 0 until size) {
                    val redByte = (r * step).toInt().toByte()
                    directBuffer.put(redByte)
                    directBuffer.put(greenByte)
                    directBuffer.put(blueByte)
                }
            }
        }
        directBuffer.position(0)

        return upload3dTexture(directBuffer, size)
    }

    private fun upload3dTexture(buffer: ByteBuffer, size: Int): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val textureId = textures[0]
        check(textureId != 0) { "Failed to generate 3D texture handle" }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, textureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE)

        // Ensure 1-byte row unpacking alignment for 3-channel RGB textures
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)

        GLES30.glTexImage3D(
            GLES30.GL_TEXTURE_3D,
            0,
            GLES30.GL_RGB8,
            size, size, size,
            0,
            GLES30.GL_RGB,
            GLES30.GL_UNSIGNED_BYTE,
            buffer
        )

        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 4)

        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, 0)
        Log.i(TAG, "Uploaded 3D LUT ($size x $size x $size) to texture ID: $textureId")
        return textureId
    }

    private fun readStreamToBuffer(input: InputStream, buffer: ByteBuffer, totalBytes: Int) {
        val temp = ByteArray(8192)
        var totalRead = 0
        while (totalRead < totalBytes) {
            val toRead = minOf(temp.size, totalBytes - totalRead)
            val read = input.read(temp, 0, toRead)
            if (read < 0) break
            buffer.put(temp, 0, read)
            totalRead += read
        }
    }

    /**
     * Deletes a texture handle deterministically.
     */
    fun releaseTexture(textureId: Int) {
        if (textureId != 0) {
            val textures = intArrayOf(textureId)
            GLES30.glDeleteTextures(1, textures, 0)
        }
    }
}
