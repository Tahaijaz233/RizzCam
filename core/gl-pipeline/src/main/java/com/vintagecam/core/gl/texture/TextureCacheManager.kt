package com.vintagecam.core.gl.texture

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log

/**
 * VRAM-safe texture lifecycle and cache manager.
 *
 * Strict Mobile Memory Budget (< 1.0 - 1.5 GB device constraint):
 * To guarantee that texture memory never accumulates unboundedly across preset switches,
 * [TextureCacheManager] enforces single-slot texture swapping:
 * - When switching film presets, previous 3D LUT and 2D overlay textures are immediately
 *   unbound and deleted via [GLES30.glDeleteTextures].
 * - Total preset texture footprint in VRAM is strictly bounded under 15 MB:
 *     Active 3D LUT (64x64x64 RGB8): ~0.78 MB
 *     Active Light Leak (1024x768 RGBA8): ~3.14 MB
 *     Active Border Mask (1024x768 RGBA8): ~3.14 MB
 *     Total VRAM: ~7.06 MB <= 15 MB threshold.
 */
class TextureCacheManager {
    companion object {
        private const val TAG = "TextureCacheManager"
    }

    private var activeLutTextureId: Int = 0
    private var activeLutAssetPath: String? = null

    private var activeLightLeakTextureId: Int = 0
    private var activeLightLeakAssetPath: String? = null

    private var activeBorderTextureId: Int = 0
    private var activeBorderAssetPath: String? = null

    private val singleIdBuffer = IntArray(1)

    /**
     * Loads a binary .raw3d 3D LUT asset. If already active, returns cached ID without reloading.
     * If switching presets, automatically deletes the previous 3D texture to reclaim VRAM.
     */
    fun getOrLoadLut3d(
        assetManager: AssetManager,
        assetPath: String,
        lutSize: Int = 64
    ): Int {
        if (assetPath == activeLutAssetPath && activeLutTextureId != 0) {
            return activeLutTextureId
        }

        // Delete previous 3D LUT texture from GPU memory
        if (activeLutTextureId != 0) {
            deleteTexture(activeLutTextureId)
            activeLutTextureId = 0
            activeLutAssetPath = null
        }

        activeLutTextureId = Lut3dTextureLoader.loadFromAssets(assetManager, assetPath, lutSize)
        activeLutAssetPath = assetPath
        Log.d(TAG, "Loaded 3D LUT: $assetPath (texId=$activeLutTextureId, size=${lutSize}^3)")
        return activeLutTextureId
    }

    /**
     * Loads or caches a 2D Light Leak overlay texture.
     */
    fun getOrLoadLightLeak(
        assetManager: AssetManager,
        assetPath: String?
    ): Int {
        if (assetPath == null) {
            if (activeLightLeakTextureId != 0) {
                deleteTexture(activeLightLeakTextureId)
                activeLightLeakTextureId = 0
                activeLightLeakAssetPath = null
            }
            return 0
        }

        if (assetPath == activeLightLeakAssetPath && activeLightLeakTextureId != 0) {
            return activeLightLeakTextureId
        }

        if (activeLightLeakTextureId != 0) {
            deleteTexture(activeLightLeakTextureId)
            activeLightLeakTextureId = 0
            activeLightLeakAssetPath = null
        }

        activeLightLeakTextureId = load2dTextureFromAsset(assetManager, assetPath)
        activeLightLeakAssetPath = assetPath
        return activeLightLeakTextureId
    }

    /**
     * Loads or caches a 2D Border Mask overlay texture.
     */
    fun getOrLoadBorderMask(
        assetManager: AssetManager,
        assetPath: String?
    ): Int {
        if (assetPath == null) {
            if (activeBorderTextureId != 0) {
                deleteTexture(activeBorderTextureId)
                activeBorderTextureId = 0
                activeBorderAssetPath = null
            }
            return 0
        }

        if (assetPath == activeBorderAssetPath && activeBorderTextureId != 0) {
            return activeBorderTextureId
        }

        if (activeBorderTextureId != 0) {
            deleteTexture(activeBorderTextureId)
            activeBorderTextureId = 0
            activeBorderAssetPath = null
        }

        activeBorderTextureId = load2dTextureFromAsset(assetManager, assetPath)
        activeBorderAssetPath = assetPath
        return activeBorderTextureId
    }

    private fun load2dTextureFromAsset(assetManager: AssetManager, assetPath: String): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val texId = textures[0]

        assetManager.open(assetPath).use { stream ->
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeStream(stream, null, options) ?: throw RuntimeException("Failed to decode asset: $assetPath")
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmap.recycle() // Recycle native bitmap memory immediately after GPU upload
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return texId
    }

    private fun deleteTexture(texId: Int) {
        singleIdBuffer[0] = texId
        GLES30.glDeleteTextures(1, singleIdBuffer, 0)
    }

    /**
     * Releases all cached GPU textures and reclaims all texture VRAM.
     */
    fun releaseAll() {
        if (activeLutTextureId != 0) {
            deleteTexture(activeLutTextureId)
            activeLutTextureId = 0
            activeLutAssetPath = null
        }
        if (activeLightLeakTextureId != 0) {
            deleteTexture(activeLightLeakTextureId)
            activeLightLeakTextureId = 0
            activeLightLeakAssetPath = null
        }
        if (activeBorderTextureId != 0) {
            deleteTexture(activeBorderTextureId)
            activeBorderTextureId = 0
            activeBorderAssetPath = null
        }
    }
}
