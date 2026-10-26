package com.vintagecam.core.gl

import android.content.res.AssetManager
import android.opengl.GLES30
import com.vintagecam.core.gl.fbo.FramebufferObject
import com.vintagecam.core.gl.fbo.PingPongFboPool
import com.vintagecam.core.gl.pass.AccessoryConfig
import com.vintagecam.core.gl.pass.AccessoryPass
import com.vintagecam.core.gl.pass.AccessoryType
import com.vintagecam.core.gl.pass.ChromaticAberrationPass
import com.vintagecam.core.gl.pass.CompositingPass
import com.vintagecam.core.gl.pass.FilmGrainPass
import com.vintagecam.core.gl.pass.HalationChainPass
import com.vintagecam.core.gl.pass.Lut3dPass
import com.vintagecam.core.gl.pass.OesIngestPass
import com.vintagecam.core.common.model.WbParams
import com.vintagecam.domain.presets.model.CameraPreset

/**
 * Orchestrator for the 6-pass vintage camera rendering engine.
 *
 * Pipeline Flow:
 * - PASS 0: OES Ingest & Aspect Crop (SurfaceTexture / Camera2 YUV -> FBO_A RGBA16F)
 * - PASS 1: 3D LUT Color Remapping (FBO_A + 3D LUT -> FBO_B)
 * - PASS 2: Radial Chromatic Aberration & Lens Distortion (FBO_B -> FBO_A)
 * - PASS 3: Optical Halation Sub-Pipeline (Quarter-res threshold -> 9-tap H-blur -> 9-tap V-blur -> Warm composite into FBO_B)
 * - PASS 4: Procedural Simplex Film Grain (FBO_B -> FBO_A)
 * - PASS 5: Final Compositing (FBO_A + Vignette + Light Leaks + Border -> Default Screen Surface or Offscreen Capture FBO)
 *
 * Memory & Real-Time Performance:
 * Zero heap allocations are permitted during [executeChain] to guarantee consistent
 * sub-16.6ms 60 FPS frame times and avoid garbage collector pauses.
 */
class VintageGlPipeline(
    val oesIngestPass: OesIngestPass,
    val lut3dPass: Lut3dPass,
    val chromaticAberrationPass: ChromaticAberrationPass,
    val halationChainPass: HalationChainPass,
    val filmGrainPass: FilmGrainPass,
    val compositingPass: CompositingPass,
    val accessoryPass: AccessoryPass? = null
) {
    companion object {
        fun createFromAssets(assetManager: AssetManager): VintageGlPipeline {
            val oes = OesIngestPass.createFromAssets(assetManager)
            val lut = Lut3dPass.createFromAssets(assetManager)
            val aberration = ChromaticAberrationPass.createFromAssets(assetManager)
            val halation = HalationChainPass.createFromAssets(assetManager)
            val grain = FilmGrainPass.createFromAssets(assetManager)
            val compositing = CompositingPass.createFromAssets(assetManager)
            val accessory = AccessoryPass.createFromAssets(assetManager)
            return VintageGlPipeline(oes, lut, aberration, halation, grain, compositing, accessory)
        }
    }

    /**
     * Executes the complete multi-pass rendering chain.
     *
     * @param inputTextureId Source texture handle (GL_TEXTURE_EXTERNAL_OES for camera preview, GL_TEXTURE_2D for still capture).
     * @param isExternalOes True when sampling from Android CameraX/Camera2 SurfaceTexture.
     * @param texTransformMatrix 4x4 UV transformation matrix supplied by SurfaceTexture.getTransformMatrix() concatenated with aspect crop.
     * @param pool The [PingPongFboPool] holding read/write full-res and quarter-res framebuffers.
     * @param preset Active analog film simulation preset.
     * @param lut3dTextureId Bound 3D LUT texture handle or 2D HaldCLUT handle.
     * @param lightLeakTextureId Optional texture handle for light leak overlays.
     * @param borderMaskTextureId Optional texture handle for film borders / sprocket masks.
     * @param exposureEv User manual exposure compensation in EV stops [-2.0, +2.0].
     * @param timeSeed Frame-varying seed for procedural simplex grain animation.
     * @param viewportWidth Width of target viewport.
     * @param viewportHeight Height of target viewport.
     * @param outputFbo If null, renders directly to the default window surface (0) for viewfinder display.
     *                  If specified, renders into [outputFbo] for high-res photo capture.
     */
    fun executeChain(
        inputTextureId: Int,
        isExternalOes: Boolean,
        texTransformMatrix: FloatArray,
        pool: PingPongFboPool,
        preset: CameraPreset,
        lut3dTextureId: Int,
        lightLeakTextureId: Int = 0,
        borderMaskTextureId: Int = 0,
        exposureEv: Float = 0.0f,
        timeSeed: Float = 0.0f,
        viewportWidth: Int,
        viewportHeight: Int,
        outputFbo: FramebufferObject? = null,
        lightLeakOpacity: Float = 0.0f,
        lightLeakTransformX: Float = 0.0f,
        lightLeakTransformY: Float = 0.0f,
        accessoryConfig: AccessoryConfig = AccessoryConfig(),
        wbParams: WbParams? = null
    ) {
        pool.reset()

        // ---------------------------------------------------------------------------------
        // PASS 0: OES-to-2D Ingestion, Sensor Rotation, Aspect Crop & Pre-Exposure
        // ---------------------------------------------------------------------------------
        pool.writeFbo.bind()
        oesIngestPass.render(
            inputTextureId = inputTextureId,
            isExternalOes = isExternalOes,
            texMatrix = texTransformMatrix,
            exposureEv = exposureEv,
            temperatureShift = preset.lut.temperatureShift,
            tintShift = preset.lut.tintShift,
            userTemperature = wbParams?.temperature ?: 6500f,
            userTint = wbParams?.tint ?: 0f,
            userStrength = wbParams?.strength ?: 0f
        )
        pool.swap()

        // ---------------------------------------------------------------------------------
        // PASS 1: 3D LUT Color Remapping
        // ---------------------------------------------------------------------------------
        pool.writeFbo.bind()
        lut3dPass.render(
            inputTexture2d = pool.readFbo.textureId,
            lutTextureId = lut3dTextureId,
            lutSize = preset.lut.lutSize.toFloat(),
            intensity = preset.lut.defaultIntensity,
            useHaldClut = preset.lut.isHaldClut
        )
        pool.swap()

        // ---------------------------------------------------------------------------------
        // PASS 2: Radial Chromatic Aberration & Lens Distortion
        // ---------------------------------------------------------------------------------
        pool.writeFbo.bind()
        chromaticAberrationPass.render(
            inputTexture2d = pool.readFbo.textureId,
            width = pool.width.toFloat(),
            height = pool.height.toFloat(),
            config = preset.aberration
        )
        pool.swap()

        // ---------------------------------------------------------------------------------
        // PASS 2B: Optical Accessories (Prism / Star Filter / Fisheye)
        // ---------------------------------------------------------------------------------
        if (accessoryPass != null && accessoryConfig.type != AccessoryType.NONE) {
            pool.writeFbo.bind()
            accessoryPass.render(
                inputTexture2d = pool.readFbo.textureId,
                width = pool.width.toFloat(),
                height = pool.height.toFloat(),
                config = accessoryConfig
            )
            pool.swap()
        }

        // ---------------------------------------------------------------------------------
        // PASS 3: Optical Halation Sub-Pipeline (Quarter-res bloom & warm screen composite)
        // ---------------------------------------------------------------------------------
        if (preset.halation.enabled) {
            halationChainPass.render(
                baseInputTexture2d = pool.readFbo.textureId,
                quarterFbo1 = pool.quarterFbo1,
                quarterFbo2 = pool.quarterFbo2,
                targetWriteFbo = pool.writeFbo,
                config = preset.halation
            )
            pool.swap()
        }

        // ---------------------------------------------------------------------------------
        // PASS 4: Procedural Simplex Film Grain & Midtone Soft-Light Blend
        // ---------------------------------------------------------------------------------
        pool.writeFbo.bind()
        filmGrainPass.render(
            inputTexture2d = pool.readFbo.textureId,
            width = pool.width.toFloat(),
            height = pool.height.toFloat(),
            timeSeed = timeSeed,
            config = preset.grain
        )
        pool.swap()

        // ---------------------------------------------------------------------------------
        // PASS 5: Final Optical Compositing (Vignette + Light Leak + Film Border Mask)
        // ---------------------------------------------------------------------------------
        val targetWidth: Float
        val targetHeight: Float
        if (outputFbo != null) {
            outputFbo.bind()
            targetWidth = outputFbo.width.toFloat()
            targetHeight = outputFbo.height.toFloat()
        } else {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
            targetWidth = viewportWidth.toFloat()
            targetHeight = viewportHeight.toFloat()
        }

        compositingPass.render(
            inputTexture2d = pool.readFbo.textureId,
            lightLeakTexture2d = lightLeakTextureId,
            borderMaskTexture2d = borderMaskTextureId,
            width = targetWidth,
            height = targetHeight,
            config = preset.compositing,
            lightLeakOpacity = lightLeakOpacity,
            lightLeakTransformX = lightLeakTransformX,
            lightLeakTransformY = lightLeakTransformY
        )
    }

    /**
     * Deterministically releases all shader programs and render passes.
     */
    fun release() {
        oesIngestPass.release()
        lut3dPass.release()
        chromaticAberrationPass.release()
        accessoryPass?.release()
        halationChainPass.release()
        filmGrainPass.release()
        compositingPass.release()
    }
}
