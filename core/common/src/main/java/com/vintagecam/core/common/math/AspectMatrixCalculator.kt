package com.vintagecam.core.common.math

import com.vintagecam.domain.presets.model.AspectRatioProfile

/**
 * Calculates zero-stutter aspect ratio crop matrices for Pass 0.
 *
 * Implements Section 1.2 Aspect-Ratio & Crop Mathematics:
 * Center-crops native 4:3 sensor readouts into 3:2, 1:1, 6:7, or 3:4 half-frame diptych
 * without reallocating camera sessions.
 *
 * Memory Safety:
 * Reuses internal scratch arrays to avoid GC allocations during 60 FPS viewfinder streaming.
 */
class AspectMatrixCalculator {
    private val cropMatrix = FloatArray(16)

    /**
     * Computes the combined transformation matrix: M_final = M_st * M_crop
     *
     * @param stMatrix 4x4 SurfaceTexture transform matrix from CameraX/Camera2 (16 floats)
     * @param srcWidth Native sensor stream width (post-rotation)
     * @param srcHeight Native sensor stream height (post-rotation)
     * @param targetProfile Destination aspect ratio profile
     * @param outMatrix Resulting 4x4 matrix (16 floats)
     */
    fun calculate(
        stMatrix: FloatArray,
        srcWidth: Int,
        srcHeight: Int,
        targetProfile: AspectRatioProfile,
        outMatrix: FloatArray
    ) {
        val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
        val dstAspect = targetProfile.aspectRatio

        val sx = if (dstAspect < srcAspect) dstAspect / srcAspect else 1.0f
        val sy = if (dstAspect > srcAspect) srcAspect / dstAspect else 1.0f

        // Reset to identity
        setIdentityMatrix(cropMatrix)

        // Center-crop scale and translate: M_crop[0]=Sx, M_crop[5]=Sy, M_crop[12]=(1-Sx)/2, M_crop[13]=(1-Sy)/2
        cropMatrix[0] = sx
        cropMatrix[5] = sy
        cropMatrix[12] = (1.0f - sx) * 0.5f
        cropMatrix[13] = (1.0f - sy) * 0.5f

        // M_final = M_st * M_crop
        multiplyMatrices(outMatrix, stMatrix, cropMatrix)
    }

    /**
     * Computes crop scale factors (Sx, Sy) and translations (Tx, Ty) directly.
     */
    fun computeCropFactors(
        srcWidth: Int,
        srcHeight: Int,
        targetProfile: AspectRatioProfile
    ): CropFactors {
        val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
        val dstAspect = targetProfile.aspectRatio

        val sx = if (dstAspect < srcAspect) dstAspect / srcAspect else 1.0f
        val sy = if (dstAspect > srcAspect) srcAspect / dstAspect else 1.0f
        val tx = (1.0f - sx) * 0.5f
        val ty = (1.0f - sy) * 0.5f

        return CropFactors(sx, sy, tx, ty)
    }

    data class CropFactors(val sx: Float, val sy: Float, val tx: Float, val ty: Float)

    companion object {
        fun setIdentityMatrix(m: FloatArray) {
            for (i in 0 until 16) {
                m[i] = if (i % 5 == 0) 1.0f else 0.0f
            }
        }

        fun multiplyMatrices(result: FloatArray, lhs: FloatArray, rhs: FloatArray) {
            for (i in 0 until 4) {
                for (j in 0 until 4) {
                    var sum = 0.0f
                    for (k in 0 until 4) {
                        sum += lhs[k * 4 + i] * rhs[j * 4 + k]
                    }
                    result[j * 4 + i] = sum
                }
            }
        }
    }
}
