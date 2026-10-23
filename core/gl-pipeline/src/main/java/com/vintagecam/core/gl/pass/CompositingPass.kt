package com.vintagecam.core.gl.pass

import android.content.res.AssetManager
import android.opengl.GLES30
import com.vintagecam.core.gl.shader.GlProgram
import com.vintagecam.domain.presets.model.CompositingConfig

/**
 * PASS 5: Final Optical Compositing.
 *
 * Applies:
 * 1. Physical cos^4(theta) optical vignette falloff.
 * 2. Organic screen-blended light leaks with randomized per-shot UV flipping.
 * 3. Premultiplied alpha retro film borders, Polaroid chins, and sprocket masks.
 */
class CompositingPass(
    program: GlProgram
) : GlRenderPass(program) {

    companion object {
        fun createFromAssets(assetManager: AssetManager): CompositingPass {
            val program = GlProgram.createFromAssets(
                assetManager,
                "shaders/fullscreen_quad.vert",
                "shaders/compositing.frag"
            )
            return CompositingPass(program)
        }
    }

    fun render(
        inputTexture2d: Int,
        lightLeakTexture2d: Int,
        borderMaskTexture2d: Int,
        width: Float,
        height: Float,
        config: CompositingConfig,
        lightLeakOpacity: Float = 0.0f,
        lightLeakTransformX: Float = 0.0f,
        lightLeakTransformY: Float = 0.0f
    ) {
        program.use()

        program.setUniformMatrix4fv("uTexMatrix", IDENTITY_MATRIX)
        program.setTexture("uInputTexture", 0, inputTexture2d, GLES30.GL_TEXTURE_2D)

        if (lightLeakTexture2d != 0 && lightLeakOpacity > 0.001f) {
            program.setTexture("uLightLeakTexture", 1, lightLeakTexture2d, GLES30.GL_TEXTURE_2D)
            program.setUniform1f("uLightLeakOpacity", lightLeakOpacity)
            program.setUniform2f("uLightLeakUvTransform", lightLeakTransformX, lightLeakTransformY)
        } else {
            program.setUniform1f("uLightLeakOpacity", 0.0f)
            program.setUniform2f("uLightLeakUvTransform", 0.0f, 0.0f)
        }

        val enableBorder = if (borderMaskTexture2d != 0 && config.borderMaskAsset != null) 1 else 0
        if (enableBorder == 1) {
            program.setTexture("uBorderMaskTexture", 2, borderMaskTexture2d, GLES30.GL_TEXTURE_2D)
        }
        program.setUniform1i("uEnableBorderMask", enableBorder)

        program.setUniform2f("uResolution", width, height)
        program.setUniform1f("uVignetteIntensity", config.vignetteIntensity)
        program.setUniform1f("uVignetteSmoothness", config.vignetteSmoothness)

        drawFullscreenQuad()
    }
}
