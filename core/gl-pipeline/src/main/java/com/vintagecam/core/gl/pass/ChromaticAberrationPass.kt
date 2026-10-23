package com.vintagecam.core.gl.pass

import android.content.res.AssetManager
import android.opengl.GLES30
import com.vintagecam.core.gl.shader.GlProgram
import com.vintagecam.domain.presets.model.AberrationConfig

/**
 * PASS 2: Radial Chromatic Aberration & Lens Distortion.
 *
 * Simulates vintage single-element plastic or uncoated glass optics using a Brown-Conrady
 * radial polynomial with wavelength dispersion offsets (expanding Red outward and contracting
 * Blue inward) along with 3-tap sub-stepped radial filtering.
 */
class ChromaticAberrationPass(
    program: GlProgram
) : GlRenderPass(program) {

    companion object {
        fun createFromAssets(assetManager: AssetManager): ChromaticAberrationPass {
            val program = GlProgram.createFromAssets(
                assetManager,
                "shaders/fullscreen_quad.vert",
                "shaders/chromatic_aberration.frag"
            )
            return ChromaticAberrationPass(program)
        }
    }

    fun render(
        inputTexture2d: Int,
        width: Float,
        height: Float,
        config: AberrationConfig
    ) {
        program.use()

        program.setUniformMatrix4fv("uTexMatrix", IDENTITY_MATRIX)
        program.setTexture("uInputTexture", 0, inputTexture2d, GLES30.GL_TEXTURE_2D)
        program.setUniform2f("uResolution", width, height)
        program.setUniform1f("uAberrationStrength", config.strength)
        program.setUniform1f("uCubicDistortion", config.cubicDistortion)
        program.setUniform1f("uFalloffPower", config.falloffPower)

        drawFullscreenQuad()
    }
}
