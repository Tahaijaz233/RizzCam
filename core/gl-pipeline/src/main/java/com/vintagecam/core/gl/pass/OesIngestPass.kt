package com.vintagecam.core.gl.pass

import android.content.res.AssetManager
import android.opengl.GLES11Ext
import android.opengl.GLES30
import com.vintagecam.core.gl.shader.GlProgram

/**
 * PASS 0: OES Ingestion, Pre-Exposure & Sensor Transformation.
 *
 * Ingests Camera2 hardware YUV frames from [GLES11Ext.GL_TEXTURE_EXTERNAL_OES], applies
 * the 4x4 sensor rotation + aspect ratio crop matrix via [uTexMatrix], and performs
 * linear-approximated exposure and white balance shifts before LUT color grading.
 */
class OesIngestPass(
    private val oesProgram: GlProgram,
    private val standard2dProgram: GlProgram
) : GlRenderPass(oesProgram) {

    companion object {
        fun createFromAssets(assetManager: AssetManager): OesIngestPass {
            val oesProgram = GlProgram.createFromAssets(
                assetManager,
                "shaders/fullscreen_quad.vert",
                "shaders/oes_ingest.frag"
            )
            val standard2dProgram = GlProgram.createFromAssets(
                assetManager,
                "shaders/fullscreen_quad.vert",
                "shaders/standard_2d_ingest.frag"
            )
            return OesIngestPass(oesProgram, standard2dProgram)
        }
    }

    /**
     * Executes Pass 0 rendering.
     */
    fun render(
        inputTextureId: Int,
        isExternalOes: Boolean,
        texMatrix: FloatArray,
        exposureEv: Float,
        temperatureShift: Float,
        tintShift: Float
    ) {
        val activeProgram = if (isExternalOes) oesProgram else standard2dProgram
        activeProgram.use()

        if (isExternalOes) {
            activeProgram.setTexture("uOesTexture", 0, inputTextureId, GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
        } else {
            activeProgram.setTexture("u2dTexture", 0, inputTextureId, GLES30.GL_TEXTURE_2D)
        }

        activeProgram.setUniformMatrix4fv("uTexMatrix", texMatrix)
        activeProgram.setUniform1f("uExposureEv", exposureEv)
        activeProgram.setUniform1f("uTemperatureShift", temperatureShift)
        activeProgram.setUniform1f("uTintShift", tintShift)

        drawFullscreenQuad()
    }

    override fun release() {
        super.release()
        standard2dProgram.release()
    }
}
