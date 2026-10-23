package com.vintagecam.core.gl.pass

import android.content.res.AssetManager
import android.opengl.GLES30
import com.vintagecam.core.gl.shader.GlProgram
import com.vintagecam.domain.presets.model.GrainConfig

/**
 * PASS 4: Procedural Simplex Film Grain & Luminance Response.
 *
 * Implements two-octave Ashima simplex noise with:
 * - Preview vs capture resolution normalization (referenced to 1080p short edge).
 * - Organic silver-halide clumping and optional dye-cloud chroma noise.
 * - Midtone-weighted bell-curve response (peaking at ~0.45 luma, suppressed in highlights & shadows).
 * - Pegtop soft-light blend mode.
 */
class FilmGrainPass(
    program: GlProgram
) : GlRenderPass(program) {

    companion object {
        fun createFromAssets(assetManager: AssetManager): FilmGrainPass {
            val program = GlProgram.createFromAssets(
                assetManager,
                "shaders/fullscreen_quad.vert",
                "shaders/film_grain.frag"
            )
            return FilmGrainPass(program)
        }
    }

    fun render(
        inputTexture2d: Int,
        width: Float,
        height: Float,
        timeSeed: Float,
        config: GrainConfig
    ) {
        program.use()

        program.setUniformMatrix4fv("uTexMatrix", IDENTITY_MATRIX)
        program.setTexture("uInputTexture", 0, inputTexture2d, GLES30.GL_TEXTURE_2D)
        program.setUniform2f("uResolution", width, height)
        program.setUniform1f("uTimeSeed", timeSeed)
        program.setUniform1f("uGrainIntensity", config.intensity)
        program.setUniform1f("uGrainSize", config.particleSize)
        program.setUniform1f("uColorGrain", config.colorNoiseRatio)

        drawFullscreenQuad()
    }
}
