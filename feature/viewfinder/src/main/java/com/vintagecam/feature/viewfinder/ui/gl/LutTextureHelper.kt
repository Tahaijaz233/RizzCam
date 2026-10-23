package com.vintagecam.feature.viewfinder.ui.gl

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import com.vintagecam.core.gl.texture.Lut3dTextureLoader
import com.vintagecam.domain.presets.model.LutConfig

/**
 * Utility responsible for loading raw 3D LUT binaries and 2D HaldCLUT image assets
 * into OpenGL ES 3.0 textures.
 *
 * Delegates 3D LUT loading and neutral LUT synthesis to canonical [Lut3dTextureLoader]
 * in `core:gl-pipeline`.
 */
object LutTextureHelper {

    private const val TAG = "LutTextureHelper"
    private val textureCache = mutableMapOf<String, Int>()

    /**
     * Loads or retrieves a cached LUT texture ID for the specified [LutConfig].
     */
    fun loadLutTexture(assetManager: AssetManager, config: LutConfig): Int {
        val cacheKey = config.assetPath
        textureCache[cacheKey]?.let { cachedId ->
            if (cachedId != 0 && GLES30.glIsTexture(cachedId)) {
                return cachedId
            }
        }

        val textureId = if (config.isHaldClut) {
            load2dHaldClut(assetManager, config.assetPath)
        } else {
            load3dRawLut(assetManager, config.assetPath, config.lutSize)
        }

        textureCache[cacheKey] = textureId
        return textureId
    }

    /**
     * Loads a 64x64x64 raw RGB 3D LUT binary via [Lut3dTextureLoader].
     */
    private fun load3dRawLut(assetManager: AssetManager, assetPath: String, lutSize: Int): Int {
        return try {
            Lut3dTextureLoader.loadFromAssets(assetManager, assetPath, lutSize)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to load 3D LUT from $assetPath, synthesizing neutral identity LUT: ${t.message}")
            createNeutral3dLut(lutSize)
        }
    }

    /**
     * Loads a 2D HaldCLUT image (e.g., 512x512 PNG) into [GLES30.GL_TEXTURE_2D].
     */
    private fun load2dHaldClut(assetManager: AssetManager, assetPath: String): Int {
        try {
            assetManager.open(assetPath).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: return 0
                val texIds = IntArray(1)
                GLES30.glGenTextures(1, texIds, 0)
                val texId = texIds[0]

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
                bitmap.recycle()
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
                return texId
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to load 2D HaldCLUT from $assetPath: ${t.message}")
            return 0
        }
    }

    /**
     * Generates a neutral identity 64x64x64 3D LUT where input colors map to identical output colors.
     */
    fun createNeutral3dLut(lutSize: Int = 64): Int {
        return Lut3dTextureLoader.createIdentityLut(lutSize)
    }

    /**
     * Frees all cached OpenGL textures.
     */
    fun clearCache() {
        val ids = textureCache.values.filter { it != 0 }.toIntArray()
        if (ids.isNotEmpty()) {
            GLES30.glDeleteTextures(ids.size, ids, 0)
        }
        textureCache.clear()
    }
}
