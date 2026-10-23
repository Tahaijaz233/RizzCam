package com.vintagecam.core.gl.pass

import android.content.res.AssetManager
import android.opengl.GLES30
import com.vintagecam.core.gl.shader.GlProgram

/**
 * Optical accessory lens attachments placed in front of the camera lens.
 */
enum class AccessoryType {
    NONE,
    PRISM,
    STAR,
    FISHEYE
}

data class PrismParams(
    val facetCount: Int = 3, // 3 or 6 facets
    val rotation: Float = 0.0f,
    val refractionStrength: Float = 0.08f,
    val chromaticSpread: Float = 0.025f,
    val centerSplit: Float = 0.12f
)

data class StarParams(
    val angle: Float = 0.7853982f, // 45 degrees
    val threshold: Float = 0.75f,
    val rayLength: Float = 0.08f,
    val intensity: Float = 0.85f
)

data class FisheyeParams(
    val curvature: Float = 0.55f,
    val vignetteRadius: Float = 0.95f,
    val vignetteSoftness: Float = 0.04f,
    val chromaticFringe: Float = 0.008f
)

data class AccessoryConfig(
    val type: AccessoryType = AccessoryType.NONE,
    val prism: PrismParams = PrismParams(),
    val star: StarParams = StarParams(),
    val fisheye: FisheyeParams = FisheyeParams()
)

/**
 * PASS: Optical Accessories & Filters.
 *
 * Implements physical filter simulations:
 * - [AccessoryType.PRISM]: 3-facet or 6-facet cut glass prism refraction with dispersion.
 * - [AccessoryType.STAR]: 4-point etched glass star cross-screen flare on specular highlights.
 * - [AccessoryType.FISHEYE]: Barrel distortion polynomial mapping with circular vignette.
 */
class AccessoryPass(
    val prismProgram: GlProgram,
    val starProgram: GlProgram,
    val fisheyeProgram: GlProgram
) : GlRenderPass(prismProgram) {

    companion object {
        fun createFromAssets(assetManager: AssetManager): AccessoryPass {
            val prism = GlProgram.createFromAssets(
                assetManager,
                "shaders/fullscreen_quad.vert",
                "shaders/accessory_prism.frag"
            )
            val star = GlProgram.createFromAssets(
                assetManager,
                "shaders/fullscreen_quad.vert",
                "shaders/accessory_star.frag"
            )
            val fisheye = GlProgram.createFromAssets(
                assetManager,
                "shaders/fullscreen_quad.vert",
                "shaders/accessory_fisheye.frag"
            )
            return AccessoryPass(prism, star, fisheye)
        }
    }

    /**
     * Renders the optical accessory effect. If [accessoryType] is [AccessoryType.NONE],
     * this pass does not draw.
     */
    fun render(
        inputTexture2d: Int,
        width: Float,
        height: Float,
        accessoryType: AccessoryType,
        prismParams: PrismParams = PrismParams(),
        starParams: StarParams = StarParams(),
        fisheyeParams: FisheyeParams = FisheyeParams()
    ) {
        when (accessoryType) {
            AccessoryType.NONE -> return
            AccessoryType.PRISM -> {
                prismProgram.use()
                prismProgram.setUniformMatrix4fv("uTexMatrix", IDENTITY_MATRIX)
                prismProgram.setTexture("uInputTexture", 0, inputTexture2d, GLES30.GL_TEXTURE_2D)
                prismProgram.setUniform2f("uResolution", width, height)
                prismProgram.setUniform1f("uFacetCount", prismParams.facetCount.toFloat())
                prismProgram.setUniform1f("uPrismRotation", prismParams.rotation)
                prismProgram.setUniform1f("uRefractionStrength", prismParams.refractionStrength)
                prismProgram.setUniform1f("uChromaticSpread", prismParams.chromaticSpread)
                prismProgram.setUniform1f("uCenterSplit", prismParams.centerSplit)
                drawFullscreenQuad()
            }
            AccessoryType.STAR -> {
                starProgram.use()
                starProgram.setUniformMatrix4fv("uTexMatrix", IDENTITY_MATRIX)
                starProgram.setTexture("uInputTexture", 0, inputTexture2d, GLES30.GL_TEXTURE_2D)
                starProgram.setUniform2f("uResolution", width, height)
                starProgram.setUniform1f("uStarAngle", starParams.angle)
                starProgram.setUniform1f("uThreshold", starParams.threshold)
                starProgram.setUniform1f("uStarLength", starParams.rayLength)
                starProgram.setUniform1f("uStarIntensity", starParams.intensity)
                drawFullscreenQuad()
            }
            AccessoryType.FISHEYE -> {
                fisheyeProgram.use()
                fisheyeProgram.setUniformMatrix4fv("uTexMatrix", IDENTITY_MATRIX)
                fisheyeProgram.setTexture("uInputTexture", 0, inputTexture2d, GLES30.GL_TEXTURE_2D)
                fisheyeProgram.setUniform2f("uResolution", width, height)
                fisheyeProgram.setUniform1f("uFisheyeCurvature", fisheyeParams.curvature)
                fisheyeProgram.setUniform1f("uCircularVignetteRadius", fisheyeParams.vignetteRadius)
                fisheyeProgram.setUniform1f("uVignetteSoftness", fisheyeParams.vignetteSoftness)
                fisheyeProgram.setUniform1f("uChromaticFringe", fisheyeParams.chromaticFringe)
                drawFullscreenQuad()
            }
        }
    }

    fun render(
        inputTexture2d: Int,
        width: Float,
        height: Float,
        config: AccessoryConfig
    ) {
        render(
            inputTexture2d = inputTexture2d,
            width = width,
            height = height,
            accessoryType = config.type,
            prismParams = config.prism,
            starParams = config.star,
            fisheyeParams = config.fisheye
        )
    }

    override fun release() {
        super.release()
        starProgram.release()
        fisheyeProgram.release()
    }
}
