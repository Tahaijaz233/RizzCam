package com.vintagecam.core.gl.pass

import android.content.res.AssetManager
import android.opengl.GLES30
import com.vintagecam.core.gl.shader.GlProgram

/**
 * PASS 1: 3D LUT Color Grading.
 *
 * Performs trilinear color lookup with half-texel boundary centering using either:
 * 1. Hardware accelerated [GLES30.GL_TEXTURE_3D] (64x64x64 cube).
 * 2. Fallback 2D HaldCLUT (512x512 containing 64 slices arranged in an 8x8 tile grid).
 */
class Lut3dPass(
    program: GlProgram
) : GlRenderPass(program) {

    companion object {
        fun createFromAssets(assetManager: AssetManager): Lut3dPass {
            val program = GlProgram.createFromAssets(
                assetManager,
                "shaders/fullscreen_quad.vert",
                "shaders/lut_filter.frag"
            )
            return Lut3dPass(program)
        }
    }

    fun render(
        inputTexture2d: Int,
        lutTextureId: Int,
        lutSize: Float = 64.0f,
        intensity: Float = 1.0f,
        useHaldClut: Boolean = false
    ) {
        program.use()

        program.setUniformMatrix4fv("uTexMatrix", IDENTITY_MATRIX)
        program.setTexture("uInputTexture", 0, inputTexture2d, GLES30.GL_TEXTURE_2D)

        if (useHaldClut) {
            program.setTexture("uHaldLutTexture", 2, lutTextureId, GLES30.GL_TEXTURE_2D)
            program.setUniform1i("uUseHaldClut", 1)
        } else {
            program.setTexture("uLut3dTexture", 1, lutTextureId, GLES30.GL_TEXTURE_3D)
            program.setUniform1i("uUseHaldClut", 0)
        }

        program.setUniform1f("uLutSize", lutSize)
        program.setUniform1f("uLutIntensity", intensity)

        drawFullscreenQuad()
    }
}
