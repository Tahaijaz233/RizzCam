package com.vintagecam.core.gl.shader

import android.content.res.AssetManager
import android.opengl.GLES30
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * OpenGL ES 3.0 Program wrapper.
 *
 * Handles:
 * - Deterministic compilation and shader link status validation.
 * - Robust error diagnosis via [GLES30.glGetShaderInfoLog] & [GLES30.glGetProgramInfoLog].
 * - High-speed cached uniform location resolution to eliminate JNI lookup overhead in render loops.
 * - Resource teardown via [release].
 */
class GlProgram(
    vertexSource: String,
    fragmentSource: String
) {
    companion object {
        private const val TAG = "GlProgram"

        /**
         * Utility to read shader source code from an Android [AssetManager].
         */
        fun readShaderAsset(assetManager: AssetManager, assetPath: String): String {
            return assetManager.open(assetPath).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        }

        /**
         * Factory constructor loading shader sources directly from assets.
         */
        fun createFromAssets(
            assetManager: AssetManager,
            vertexAssetPath: String,
            fragmentAssetPath: String
        ): GlProgram {
            val vertSource = readShaderAsset(assetManager, vertexAssetPath)
            val fragSource = readShaderAsset(assetManager, fragmentAssetPath)
            return GlProgram(vertSource, fragSource)
        }
    }

    var programId: Int = 0
        private set

    private val uniformLocations = HashMap<String, Int>()
    private val attribLocations = HashMap<String, Int>()

    init {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)

        programId = GLES30.glCreateProgram()
        check(programId != 0) { "glCreateProgram failed" }

        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)
        GLES30.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES30.GL_TRUE) {
            val infoLog = GLES30.glGetProgramInfoLog(programId)
            GLES30.glDeleteProgram(programId)
            programId = 0
            throw RuntimeException("Could not link program:\n$infoLog")
        }

        // Flag shaders for deletion once program is linked
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
    }

    private fun compileShader(shaderType: Int, source: String): Int {
        val shader = GLES30.glCreateShader(shaderType)
        check(shader != 0) { "Could not create shader of type $shaderType" }

        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val info = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            val typeStr = if (shaderType == GLES30.GL_VERTEX_SHADER) "VERTEX" else "FRAGMENT"
            throw RuntimeException("Compilation failed for $typeStr shader:\n$info\nSource:\n$source")
        }
        return shader
    }

    /**
     * Binds this program for execution.
     */
    fun use() {
        GLES30.glUseProgram(programId)
    }

    /**
     * Retrieves cached uniform location.
     */
    fun getUniformLocation(name: String): Int {
        return uniformLocations.getOrPut(name) {
            val loc = GLES30.glGetUniformLocation(programId, name)
            if (loc < 0) {
                Log.d(TAG, "Uniform '$name' not found or active in program $programId")
            }
            loc
        }
    }

    /**
     * Retrieves cached attribute location.
     */
    fun getAttribLocation(name: String): Int {
        return attribLocations.getOrPut(name) {
            val loc = GLES30.glGetAttribLocation(programId, name)
            if (loc < 0) {
                Log.d(TAG, "Attribute '$name' not found in program $programId")
            }
            loc
        }
    }

    fun setUniform1i(name: String, value: Int) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform1i(loc, value)
    }

    fun setUniform1f(name: String, value: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform1f(loc, value)
    }

    fun setUniform2f(name: String, v0: Float, v1: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform2f(loc, v0, v1)
    }

    fun setUniform3f(name: String, v0: Float, v1: Float, v2: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform3f(loc, v0, v1, v2)
    }

    fun setUniform4f(name: String, v0: Float, v1: Float, v2: Float, v3: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform4f(loc, v0, v1, v2, v3)
    }

    fun setUniformMatrix4fv(name: String, matrix: FloatArray) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniformMatrix4fv(loc, 1, false, matrix, 0)
    }

    /**
     * Binds a 2D or 3D texture to the specified texture unit index and updates the uniform sampler.
     */
    fun setTexture(name: String, textureUnitIndex: Int, textureId: Int, target: Int = GLES30.GL_TEXTURE_2D) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnitIndex)
        GLES30.glBindTexture(target, textureId)
        setUniform1i(name, textureUnitIndex)
    }

    /**
     * Releases OpenGL program resources immediately.
     */
    fun release() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
            uniformLocations.clear()
            attribLocations.clear()
        }
    }
}
