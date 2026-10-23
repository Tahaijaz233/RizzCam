package com.vintagecam.core.gl.pass

import android.content.res.AssetManager
import android.opengl.GLES30
import com.vintagecam.core.gl.fbo.FramebufferObject
import com.vintagecam.core.gl.shader.GlProgram
import com.vintagecam.domain.presets.model.HalationConfig

/**
 * PASS 3: Optical Halation & Glow Sub-Pipeline.
 *
 * Coordinates the 4-stage halation chain:
 * 1. Pass 3a: High-Luminance Soft-Knee Threshold (rendered at 1/4 res into quarterFbo1).
 * 2. Pass 3b: Separable 9-tap Gaussian Blur Horizontal (quarterFbo1 -> quarterFbo2).
 * 3. Pass 3c: Separable 9-tap Gaussian Blur Vertical (quarterFbo2 -> quarterFbo1).
 * 4. Pass 3d: Warm Red-Orange Additive Screen Blend (base input + quarterFbo1 -> target full-res FBO).
 */
class HalationChainPass(
    val thresholdProgram: GlProgram,
    val blurProgram: GlProgram,
    val compositeProgram: GlProgram
) : GlRenderPass(compositeProgram) {

    companion object {
        fun createFromAssets(assetManager: AssetManager): HalationChainPass {
            val threshold = GlProgram.createFromAssets(
                assetManager,
                "shaders/fullscreen_quad.vert",
                "shaders/halation_threshold.frag"
            )
            val blur = GlProgram.createFromAssets(
                assetManager,
                "shaders/fullscreen_quad.vert",
                "shaders/separable_blur.frag"
            )
            val composite = GlProgram.createFromAssets(
                assetManager,
                "shaders/fullscreen_quad.vert",
                "shaders/halation_composite.frag"
            )
            return HalationChainPass(threshold, blur, composite)
        }
    }

    /**
     * Executes the full halation sub-pipeline.
     */
    fun render(
        baseInputTexture2d: Int,
        quarterFbo1: FramebufferObject,
        quarterFbo2: FramebufferObject,
        targetWriteFbo: FramebufferObject,
        config: HalationConfig
    ) {
        // Step 3a: Extract highlight threshold into quarterFbo1
        quarterFbo1.bind()
        thresholdProgram.use()
        thresholdProgram.setUniformMatrix4fv("uTexMatrix", IDENTITY_MATRIX)
        thresholdProgram.setTexture("uInputTexture", 0, baseInputTexture2d, GLES30.GL_TEXTURE_2D)
        thresholdProgram.setUniform1f("uThreshold", config.threshold)
        thresholdProgram.setUniform1f("uKnee", config.knee)
        drawFullscreenQuad()

        // Step 3b: Horizontal Gaussian blur (quarterFbo1 -> quarterFbo2)
        quarterFbo2.bind()
        blurProgram.use()
        blurProgram.setUniformMatrix4fv("uTexMatrix", IDENTITY_MATRIX)
        blurProgram.setTexture("uInputTexture", 0, quarterFbo1.textureId, GLES30.GL_TEXTURE_2D)
        blurProgram.setUniform2f(
            "uTexelOffset",
            config.blurRadius / quarterFbo1.width.toFloat(),
            0.0f
        )
        drawFullscreenQuad()

        // Step 3c: Vertical Gaussian blur (quarterFbo2 -> quarterFbo1)
        quarterFbo1.bind()
        blurProgram.use()
        blurProgram.setUniformMatrix4fv("uTexMatrix", IDENTITY_MATRIX)
        blurProgram.setTexture("uInputTexture", 0, quarterFbo2.textureId, GLES30.GL_TEXTURE_2D)
        blurProgram.setUniform2f(
            "uTexelOffset",
            0.0f,
            config.blurRadius / quarterFbo1.height.toFloat()
        )
        drawFullscreenQuad()

        // Step 3d: Halation composite into targetWriteFbo (full resolution)
        targetWriteFbo.bind()
        compositeProgram.use()
        compositeProgram.setUniformMatrix4fv("uTexMatrix", IDENTITY_MATRIX)
        compositeProgram.setTexture("uBaseTexture", 0, baseInputTexture2d, GLES30.GL_TEXTURE_2D)
        compositeProgram.setTexture("uHalationBlurTexture", 1, quarterFbo1.textureId, GLES30.GL_TEXTURE_2D)

        val tint = config.tintRgb
        val r = if (tint.isNotEmpty()) tint[0] else 1.0f
        val g = if (tint.size > 1) tint[1] else 0.26f
        val b = if (tint.size > 2) tint[2] else 0.08f
        compositeProgram.setUniform3f("uHalationTint", r, g, b)
        compositeProgram.setUniform1f("uHalationStrength", config.strength)
        drawFullscreenQuad()
    }

    override fun release() {
        super.release()
        thresholdProgram.release()
        blurProgram.release()
    }
}
