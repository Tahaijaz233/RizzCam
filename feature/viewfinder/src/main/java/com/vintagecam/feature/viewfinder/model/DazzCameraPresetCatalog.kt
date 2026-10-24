package com.vintagecam.feature.viewfinder.model

import com.vintagecam.domain.presets.model.AberrationConfig
import com.vintagecam.domain.presets.model.AspectRatioProfile
import com.vintagecam.domain.presets.model.CameraPreset
import com.vintagecam.domain.presets.model.CompositingConfig
import com.vintagecam.domain.presets.model.GrainConfig
import com.vintagecam.domain.presets.model.HalationConfig
import com.vintagecam.domain.presets.model.LutConfig
import com.vintagecam.domain.presets.model.PresetCategory

/**
 * Authentic filter profiles for each camera model in the Dazz / RizzCam catalog.
 *
 * Grounded in authentic analog film chemistry and optics from Dazz Cam and vintage cameras:
 * - Specific 3D LUT profiles (Portra 400, Fuji Superia 400, CCD PowerShot, Contax T2, CineStill 800T, Holga 120, Polaroid SX-70, Aerochrome IR)
 * - Calibrated color temperature and tint shifts
 * - Physical radial chromatic aberration and distortion
 * - Highlight halation bloom (ruby cinema red, golden amber, CRT cyan, or clear)
 * - Silver halide & digital sensor grain response
 * - Optical lens vignetting, organic light leaks, and authentic film border masks
 */
object DazzCameraPresetCatalog {

    /**
     * Resolves the authentic vintage filter preset for a given [CameraModel].
     */
    fun getPresetForCamera(camera: CameraModel, basePresets: List<CameraPreset>): CameraPreset {
        val baseMap = basePresets.associateBy { it.id }

        return when (camera.id) {
            // =================================================================
            // DIGITAL
            // =================================================================
            "original" -> CameraPreset(
                id = "original",
                displayName = "Original",
                subtitle = "Natural Crisp • Mobile Digital Sensor",
                category = PresetCategory.DIGITAL,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/ccd_powershot.raw3d",
                    defaultIntensity = 0.50f,
                    temperatureShift = 0.0f,
                    tintShift = 0.0f
                ),
                aberration = AberrationConfig(strength = 0.0f, cubicDistortion = 0.0f, falloffPower = 2.0f),
                halation = HalationConfig(enabled = false),
                grain = GrainConfig(intensity = 0.0f, particleSize = 1.0f, colorNoiseRatio = 0.0f),
                compositing = CompositingConfig(vignetteIntensity = 0.0f, vignetteSmoothness = 0.90f, enableDateStamp = false)
            )

            "grd_r" -> CameraPreset(
                id = "grd_r",
                displayName = "GRD R",
                subtitle = "High-Contrast Snap • Ricoh Street Tone",
                category = PresetCategory.DIGITAL,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/ccd_powershot.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = -0.05f,
                    tintShift = -0.02f
                ),
                aberration = AberrationConfig(strength = 0.002f, cubicDistortion = -0.002f, falloffPower = 2.6f),
                halation = HalationConfig(enabled = false),
                grain = GrainConfig(intensity = 0.14f, particleSize = 1.1f, colorNoiseRatio = 0.50f),
                compositing = CompositingConfig(vignetteIntensity = 0.55f, vignetteSmoothness = 0.80f, enableDateStamp = false)
            )

            "ccd_r" -> CameraPreset(
                id = "ccd_r",
                displayName = "CCD R",
                subtitle = "Y2K Flash • Sharp CCD Colors",
                category = PresetCategory.DIGITAL,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/ccd_powershot.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.05f,
                    tintShift = 0.0f
                ),
                aberration = AberrationConfig(strength = 0.005f, cubicDistortion = -0.004f, falloffPower = 2.2f),
                halation = HalationConfig(enabled = true, threshold = 0.88f, knee = 0.10f, blurRadius = 1.4f, tintRgb = listOf(0.85f, 0.95f, 1.0f), strength = 0.28f),
                grain = GrainConfig(intensity = 0.15f, particleSize = 1.15f, colorNoiseRatio = 0.65f),
                compositing = CompositingConfig(vignetteIntensity = 0.35f, vignetteSmoothness = 0.85f, enableDateStamp = true)
            )

            "collage" -> CameraPreset(
                id = "collage",
                displayName = "Collage",
                subtitle = "Multi-Frame Sequential • Vintage Pastel",
                category = PresetCategory.DIGITAL,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 0.92f,
                    temperatureShift = 0.07f,
                    tintShift = -0.02f
                ),
                aberration = AberrationConfig(strength = 0.003f, cubicDistortion = -0.005f, falloffPower = 2.4f),
                halation = HalationConfig(enabled = true, threshold = 0.84f, knee = 0.12f, blurRadius = 1.8f, tintRgb = listOf(1.0f, 0.40f, 0.15f), strength = 0.25f),
                grain = GrainConfig(intensity = 0.09f, particleSize = 1.2f, colorNoiseRatio = 0.20f),
                compositing = CompositingConfig(vignetteIntensity = 0.40f, vignetteSmoothness = 0.80f, enableDateStamp = false)
            )

            "fxn_r" -> CameraPreset(
                id = "fxn_r",
                displayName = "FXN R",
                subtitle = "Fuji Film Simulation • Soft Tones & Warm Colors",
                category = PresetCategory.DIGITAL,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/fuji_superia_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = -0.01f,
                    tintShift = 0.02f
                ),
                aberration = AberrationConfig(strength = 0.002f, cubicDistortion = -0.002f, falloffPower = 2.4f),
                halation = HalationConfig(enabled = true, threshold = 0.82f, knee = 0.12f, blurRadius = 1.8f, tintRgb = listOf(1.0f, 0.30f, 0.10f), strength = 0.25f),
                grain = GrainConfig(intensity = 0.10f, particleSize = 1.2f, colorNoiseRatio = 0.20f),
                compositing = CompositingConfig(vignetteIntensity = 0.25f, vignetteSmoothness = 0.75f, enableDateStamp = false)
            )

            // =================================================================
            // VIDEO
            // =================================================================
            "v_classic" -> CameraPreset(
                id = "v_classic",
                displayName = "V Classic",
                subtitle = "Classic Broadcast Video • Warm Tungsten Glow",
                category = PresetCategory.RETRO_VIDEO,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/cinestill_800t.raw3d",
                    defaultIntensity = 0.85f,
                    temperatureShift = 0.04f,
                    tintShift = 0.02f
                ),
                aberration = AberrationConfig(strength = 0.006f, cubicDistortion = -0.008f, falloffPower = 2.2f),
                halation = HalationConfig(enabled = true, threshold = 0.78f, knee = 0.14f, blurRadius = 2.2f, tintRgb = listOf(1.0f, 0.30f, 0.10f), strength = 0.45f),
                grain = GrainConfig(intensity = 0.14f, particleSize = 1.4f, colorNoiseRatio = 0.30f),
                compositing = CompositingConfig(vignetteIntensity = 0.50f, vignetteSmoothness = 0.75f, enableDateStamp = false)
            )

            "original_v" -> CameraPreset(
                id = "original_v",
                displayName = "Original V",
                subtitle = "High-Def Mobile Video • Rich Natural Color",
                category = PresetCategory.RETRO_VIDEO,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 0.50f,
                    temperatureShift = 0.0f,
                    tintShift = 0.0f
                ),
                aberration = AberrationConfig(strength = 0.0f, cubicDistortion = 0.0f, falloffPower = 2.0f),
                halation = HalationConfig(enabled = false),
                grain = GrainConfig(intensity = 0.0f, particleSize = 1.0f, colorNoiseRatio = 0.0f),
                compositing = CompositingConfig(vignetteIntensity = 0.0f, vignetteSmoothness = 0.90f, enableDateStamp = false)
            )

            "dam" -> CameraPreset(
                id = "dam",
                displayName = "DAM",
                subtitle = "Action Wide Perspective • Punchy Saturation",
                category = PresetCategory.RETRO_VIDEO,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/ccd_powershot.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = -0.02f,
                    tintShift = 0.04f
                ),
                aberration = AberrationConfig(strength = 0.015f, cubicDistortion = -0.035f, falloffPower = 1.9f),
                halation = HalationConfig(enabled = false),
                grain = GrainConfig(intensity = 0.08f, particleSize = 1.2f, colorNoiseRatio = 0.25f),
                compositing = CompositingConfig(vignetteIntensity = 0.60f, vignetteSmoothness = 0.70f, enableDateStamp = false)
            )

            "16mm" -> CameraPreset(
                id = "16mm",
                displayName = "16mm",
                subtitle = "Bolex Motion Picture • 500T Halation & Grain",
                category = PresetCategory.RETRO_VIDEO,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/cinestill_800t.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = -0.06f,
                    tintShift = 0.03f
                ),
                aberration = AberrationConfig(strength = 0.008f, cubicDistortion = -0.015f, falloffPower = 2.0f),
                halation = HalationConfig(enabled = true, threshold = 0.70f, knee = 0.18f, blurRadius = 3.5f, tintRgb = listOf(1.0f, 0.18f, 0.04f), strength = 0.85f),
                grain = GrainConfig(intensity = 0.20f, particleSize = 1.9f, colorNoiseRatio = 0.35f),
                compositing = CompositingConfig(vignetteIntensity = 0.65f, vignetteSmoothness = 0.65f, enableDateStamp = false)
            )

            "8mm" -> CameraPreset(
                id = "8mm",
                displayName = "8mm",
                subtitle = "Super 8 Home Movie • Golden Halation",
                category = PresetCategory.RETRO_VIDEO,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/cinestill_800t.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.14f,
                    tintShift = 0.02f
                ),
                aberration = AberrationConfig(strength = 0.014f, cubicDistortion = -0.025f, falloffPower = 1.9f),
                halation = HalationConfig(enabled = true, threshold = 0.68f, knee = 0.20f, blurRadius = 3.8f, tintRgb = listOf(1.0f, 0.25f, 0.05f), strength = 0.80f),
                grain = GrainConfig(intensity = 0.24f, particleSize = 2.2f, colorNoiseRatio = 0.40f),
                compositing = CompositingConfig(vignetteIntensity = 0.80f, vignetteSmoothness = 0.60f, lightLeakPool = listOf("presets/leaks/leak_side_burn.png"), enableDateStamp = false)
            )

            "vhs" -> CameraPreset(
                id = "vhs",
                displayName = "VHS",
                subtitle = "Magnetic Tape Scanlines • Color Bleed",
                category = PresetCategory.RETRO_VIDEO,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/cinestill_800t.raw3d",
                    defaultIntensity = 0.90f,
                    temperatureShift = -0.08f,
                    tintShift = -0.07f
                ),
                aberration = AberrationConfig(strength = 0.022f, cubicDistortion = -0.012f, falloffPower = 2.0f),
                halation = HalationConfig(enabled = true, threshold = 0.75f, knee = 0.15f, blurRadius = 3.0f, tintRgb = listOf(0.45f, 0.85f, 1.0f), strength = 0.50f),
                grain = GrainConfig(intensity = 0.25f, particleSize = 1.5f, colorNoiseRatio = 0.75f),
                compositing = CompositingConfig(vignetteIntensity = 0.40f, vignetteSmoothness = 0.85f, enableDateStamp = true)
            )

            "kino" -> CameraPreset(
                id = "kino",
                displayName = "Kino",
                subtitle = "Arthouse Cinema • Moody Delicate Curves",
                category = PresetCategory.RETRO_VIDEO,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = -0.05f,
                    tintShift = 0.05f
                ),
                aberration = AberrationConfig(strength = 0.003f, cubicDistortion = -0.005f, falloffPower = 2.3f),
                halation = HalationConfig(enabled = true, threshold = 0.76f, knee = 0.15f, blurRadius = 2.4f, tintRgb = listOf(1.0f, 0.25f, 0.08f), strength = 0.50f),
                grain = GrainConfig(intensity = 0.12f, particleSize = 1.3f, colorNoiseRatio = 0.25f),
                compositing = CompositingConfig(vignetteIntensity = 0.55f, vignetteSmoothness = 0.75f, enableDateStamp = false)
            )

            "v_funs" -> CameraPreset(
                id = "v_funs",
                displayName = "V FunS",
                subtitle = "Playful Toy Video • Saturated Pop",
                category = PresetCategory.RETRO_VIDEO,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/fuji_superia_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.05f,
                    tintShift = -0.06f
                ),
                aberration = AberrationConfig(strength = 0.018f, cubicDistortion = -0.035f, falloffPower = 1.9f),
                halation = HalationConfig(enabled = true, threshold = 0.74f, knee = 0.16f, blurRadius = 2.5f, tintRgb = listOf(1.0f, 0.32f, 0.10f), strength = 0.45f),
                grain = GrainConfig(intensity = 0.18f, particleSize = 1.7f, colorNoiseRatio = 0.35f),
                compositing = CompositingConfig(vignetteIntensity = 0.85f, vignetteSmoothness = 0.55f, lightLeakPool = listOf("presets/leaks/leak_warm_01.png"), enableDateStamp = false)
            )

            "dcr" -> CameraPreset(
                id = "dcr",
                displayName = "DCR",
                subtitle = "MiniDV Camcorder • Millennium Edge",
                category = PresetCategory.RETRO_VIDEO,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/ccd_powershot.raw3d",
                    defaultIntensity = 0.95f,
                    temperatureShift = -0.02f,
                    tintShift = -0.02f
                ),
                aberration = AberrationConfig(strength = 0.005f, cubicDistortion = -0.004f, falloffPower = 2.3f),
                halation = HalationConfig(enabled = false),
                grain = GrainConfig(intensity = 0.10f, particleSize = 1.1f, colorNoiseRatio = 0.45f),
                compositing = CompositingConfig(vignetteIntensity = 0.30f, vignetteSmoothness = 0.85f, enableDateStamp = true)
            )

            "glow" -> CameraPreset(
                id = "glow",
                displayName = "Glow",
                subtitle = "Ethereal Dream Bloom • Soft Highlights",
                category = PresetCategory.RETRO_VIDEO,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/cinestill_800t.raw3d",
                    defaultIntensity = 0.85f,
                    temperatureShift = 0.02f,
                    tintShift = 0.08f
                ),
                aberration = AberrationConfig(strength = 0.008f, cubicDistortion = -0.010f, falloffPower = 2.1f),
                halation = HalationConfig(enabled = true, threshold = 0.62f, knee = 0.25f, blurRadius = 4.5f, tintRgb = listOf(1.0f, 0.45f, 0.75f), strength = 0.95f),
                grain = GrainConfig(intensity = 0.06f, particleSize = 1.2f, colorNoiseRatio = 0.20f),
                compositing = CompositingConfig(vignetteIntensity = 0.45f, vignetteSmoothness = 0.80f, enableDateStamp = false)
            )

            "slide_p" -> CameraPreset(
                id = "slide_p",
                displayName = "Slide P",
                subtitle = "Slide Projector • Velvety Blacks",
                category = PresetCategory.RETRO_VIDEO,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.09f,
                    tintShift = 0.01f
                ),
                aberration = AberrationConfig(strength = 0.004f, cubicDistortion = -0.006f, falloffPower = 2.4f),
                halation = HalationConfig(enabled = true, threshold = 0.80f, knee = 0.14f, blurRadius = 2.0f, tintRgb = listOf(1.0f, 0.50f, 0.15f), strength = 0.35f),
                grain = GrainConfig(intensity = 0.08f, particleSize = 1.2f, colorNoiseRatio = 0.15f),
                compositing = CompositingConfig(vignetteIntensity = 0.70f, vignetteSmoothness = 0.65f, enableDateStamp = false)
            )

            // =================================================================
            // VINTAGE 135
            // =================================================================
            "d_classic" -> CameraPreset(
                id = "d_classic",
                displayName = "D Classic",
                subtitle = "Everyday Compact Film • Clean & Natural Warmth",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.03f,
                    tintShift = 0.01f
                ),
                aberration = AberrationConfig(strength = 0.003f, cubicDistortion = 0.0f, falloffPower = 2.4f),
                halation = HalationConfig(enabled = false),
                grain = GrainConfig(intensity = 0.08f, particleSize = 1.1f, colorNoiseRatio = 0.15f),
                compositing = CompositingConfig(vignetteIntensity = 0.20f, vignetteSmoothness = 0.85f, enableDateStamp = true)
            )

            "gr_f" -> CameraPreset(
                id = "gr_f",
                displayName = "GR F",
                subtitle = "Needle-Sharp Prime • Street High Contrast",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/contax_t2.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = -0.02f,
                    tintShift = -0.01f
                ),
                aberration = AberrationConfig(strength = 0.002f, cubicDistortion = -0.004f, falloffPower = 2.5f),
                halation = HalationConfig(enabled = false),
                grain = GrainConfig(intensity = 0.10f, particleSize = 1.2f, colorNoiseRatio = 0.20f),
                compositing = CompositingConfig(vignetteIntensity = 0.50f, vignetteSmoothness = 0.80f, enableDateStamp = false)
            )

            "ct2f" -> CameraPreset(
                id = "ct2f",
                displayName = "CT2F",
                subtitle = "Carl Zeiss Sonnar 38mm • Crisp & Warm Skin Tones",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/contax_t2.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.02f,
                    tintShift = 0.02f
                ),
                aberration = AberrationConfig(strength = 0.003f, cubicDistortion = -0.004f, falloffPower = 2.5f),
                halation = HalationConfig(enabled = true, threshold = 0.78f, knee = 0.14f, blurRadius = 2.0f, tintRgb = listOf(1.0f, 0.38f, 0.15f), strength = 0.40f),
                grain = GrainConfig(intensity = 0.08f, particleSize = 1.2f, colorNoiseRatio = 0.15f),
                compositing = CompositingConfig(vignetteIntensity = 0.20f, vignetteSmoothness = 0.80f, lightLeakPool = listOf("presets/leaks/leak_warm_01.png"), enableDateStamp = true)
            )

            "d3d" -> CameraPreset(
                id = "d3d",
                displayName = "D3D",
                subtitle = "Stereo 3D Triple Lens • 90s Print Saturation",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/fuji_superia_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.05f,
                    tintShift = -0.04f
                ),
                aberration = AberrationConfig(strength = 0.012f, cubicDistortion = -0.018f, falloffPower = 2.0f),
                halation = HalationConfig(enabled = true, threshold = 0.80f, knee = 0.12f, blurRadius = 2.0f, tintRgb = listOf(1.0f, 0.32f, 0.10f), strength = 0.40f),
                grain = GrainConfig(intensity = 0.15f, particleSize = 1.5f, colorNoiseRatio = 0.30f),
                compositing = CompositingConfig(vignetteIntensity = 0.55f, vignetteSmoothness = 0.75f, enableDateStamp = true)
            )

            "135_ne" -> CameraPreset(
                id = "135_ne",
                displayName = "135 NE",
                subtitle = "Exposed 35mm Negative • Sprockets & Borders",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.06f,
                    tintShift = -0.02f
                ),
                aberration = AberrationConfig(strength = 0.004f, cubicDistortion = -0.008f, falloffPower = 2.4f),
                halation = HalationConfig(enabled = true, threshold = 0.82f, knee = 0.14f, blurRadius = 1.8f, tintRgb = listOf(1.0f, 0.32f, 0.12f), strength = 0.30f),
                grain = GrainConfig(intensity = 0.10f, particleSize = 1.25f, colorNoiseRatio = 0.20f),
                compositing = CompositingConfig(vignetteIntensity = 0.50f, vignetteSmoothness = 0.75f, borderMaskAsset = "presets/borders/35mm_sprocket.png", enableDateStamp = true)
            )

            "d_funs" -> CameraPreset(
                id = "d_funs",
                displayName = "D FunS",
                subtitle = "Disposable Plastic Lens • Light Leaks",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/fuji_superia_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.06f,
                    tintShift = -0.07f
                ),
                aberration = AberrationConfig(strength = 0.018f, cubicDistortion = -0.045f, falloffPower = 1.8f),
                halation = HalationConfig(enabled = true, threshold = 0.74f, knee = 0.18f, blurRadius = 2.8f, tintRgb = listOf(1.0f, 0.35f, 0.10f), strength = 0.50f),
                grain = GrainConfig(intensity = 0.20f, particleSize = 1.8f, colorNoiseRatio = 0.40f),
                compositing = CompositingConfig(vignetteIntensity = 0.95f, vignetteSmoothness = 0.55f, lightLeakPool = listOf("presets/leaks/leak_warm_01.png"), enableDateStamp = true)
            )

            "ir" -> CameraPreset(
                id = "ir",
                displayName = "IR",
                subtitle = "Infrared False Color • Pale Pink Foliage",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/aerochrome.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.05f,
                    tintShift = 0.04f
                ),
                aberration = AberrationConfig(strength = 0.012f, cubicDistortion = -0.018f, falloffPower = 2.1f),
                halation = HalationConfig(enabled = true, threshold = 0.65f, knee = 0.20f, blurRadius = 4.0f, tintRgb = listOf(1.0f, 0.15f, 0.45f), strength = 0.75f),
                grain = GrainConfig(intensity = 0.16f, particleSize = 1.6f, colorNoiseRatio = 0.25f),
                compositing = CompositingConfig(vignetteIntensity = 0.40f, vignetteSmoothness = 0.70f, enableDateStamp = false)
            )

            "classic_u" -> CameraPreset(
                id = "classic_u",
                displayName = "Classic U",
                subtitle = "Olympus 50mm f/1.4 • Golden Bokeh Warmth",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.14f,
                    tintShift = 0.02f
                ),
                aberration = AberrationConfig(strength = 0.004f, cubicDistortion = -0.008f, falloffPower = 2.4f),
                halation = HalationConfig(enabled = true, threshold = 0.78f, knee = 0.15f, blurRadius = 2.4f, tintRgb = listOf(1.0f, 0.45f, 0.15f), strength = 0.45f),
                grain = GrainConfig(intensity = 0.11f, particleSize = 1.3f, colorNoiseRatio = 0.20f),
                compositing = CompositingConfig(vignetteIntensity = 0.60f, vignetteSmoothness = 0.70f, enableDateStamp = true)
            )

            "dqs" -> CameraPreset(
                id = "dqs",
                displayName = "DQS",
                subtitle = "QuickSnap Disposable • Direct Flash",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/contax_t2.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.07f,
                    tintShift = -0.03f
                ),
                aberration = AberrationConfig(strength = 0.015f, cubicDistortion = -0.035f, falloffPower = 1.9f),
                halation = HalationConfig(enabled = true, threshold = 0.76f, knee = 0.15f, blurRadius = 2.2f, tintRgb = listOf(1.0f, 0.35f, 0.15f), strength = 0.45f),
                grain = GrainConfig(intensity = 0.18f, particleSize = 1.6f, colorNoiseRatio = 0.30f),
                compositing = CompositingConfig(vignetteIntensity = 0.85f, vignetteSmoothness = 0.60f, enableDateStamp = true)
            )

            "fqs_r" -> CameraPreset(
                id = "fqs_r",
                displayName = "FQS R",
                subtitle = "Emerald Fuji Tones • Twilight Neon",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/fuji_superia_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = -0.08f,
                    tintShift = -0.10f
                ),
                aberration = AberrationConfig(strength = 0.014f, cubicDistortion = -0.025f, falloffPower = 2.0f),
                halation = HalationConfig(enabled = true, threshold = 0.75f, knee = 0.14f, blurRadius = 2.5f, tintRgb = listOf(1.0f, 0.20f, 0.08f), strength = 0.55f),
                grain = GrainConfig(intensity = 0.18f, particleSize = 1.6f, colorNoiseRatio = 0.35f),
                compositing = CompositingConfig(vignetteIntensity = 0.75f, vignetteSmoothness = 0.65f, lightLeakPool = listOf("presets/leaks/leak_side_burn.png"), enableDateStamp = true)
            )

            "golf" -> CameraPreset(
                id = "golf",
                displayName = "Golf",
                subtitle = "8-Lens Sequential Action • Retro Documentary",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 0.95f,
                    temperatureShift = 0.04f,
                    tintShift = 0.0f
                ),
                aberration = AberrationConfig(strength = 0.008f, cubicDistortion = -0.015f, falloffPower = 2.1f),
                halation = HalationConfig(enabled = false),
                grain = GrainConfig(intensity = 0.14f, particleSize = 1.4f, colorNoiseRatio = 0.25f),
                compositing = CompositingConfig(vignetteIntensity = 0.50f, vignetteSmoothness = 0.75f, enableDateStamp = false)
            )

            "cpm35" -> CameraPreset(
                id = "cpm35",
                displayName = "CPM35",
                subtitle = "Mju-II Flash Snapshot • Pop Contrast",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/contax_t2.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.08f,
                    tintShift = 0.03f
                ),
                aberration = AberrationConfig(strength = 0.005f, cubicDistortion = -0.008f, falloffPower = 2.3f),
                halation = HalationConfig(enabled = true, threshold = 0.78f, knee = 0.14f, blurRadius = 2.0f, tintRgb = listOf(1.0f, 0.38f, 0.15f), strength = 0.40f),
                grain = GrainConfig(intensity = 0.12f, particleSize = 1.3f, colorNoiseRatio = 0.20f),
                compositing = CompositingConfig(vignetteIntensity = 0.70f, vignetteSmoothness = 0.65f, enableDateStamp = true)
            )

            "srx_r" -> CameraPreset(
                id = "srx_r",
                displayName = "SRX R",
                subtitle = "Retro Point-and-Shoot • Saturated Summer",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/fuji_superia_400.raw3d",
                    defaultIntensity = 0.95f,
                    temperatureShift = -0.03f,
                    tintShift = -0.05f
                ),
                aberration = AberrationConfig(strength = 0.004f, cubicDistortion = -0.006f, falloffPower = 2.4f),
                halation = HalationConfig(enabled = true, threshold = 0.80f, knee = 0.12f, blurRadius = 2.0f, tintRgb = listOf(1.0f, 0.30f, 0.10f), strength = 0.30f),
                grain = GrainConfig(intensity = 0.12f, particleSize = 1.3f, colorNoiseRatio = 0.25f),
                compositing = CompositingConfig(vignetteIntensity = 0.55f, vignetteSmoothness = 0.75f, enableDateStamp = true)
            )

            "d_half" -> CameraPreset(
                id = "d_half",
                displayName = "D Half",
                subtitle = "Olympus PEN Half Frame • Warm Muted Diptych",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/fuji_superia_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.03f,
                    tintShift = 0.01f
                ),
                aberration = AberrationConfig(strength = 0.006f, cubicDistortion = -0.012f, falloffPower = 2.1f),
                halation = HalationConfig(enabled = true, threshold = 0.78f, knee = 0.14f, blurRadius = 2.2f, tintRgb = listOf(1.0f, 0.30f, 0.10f), strength = 0.35f),
                grain = GrainConfig(intensity = 0.12f, particleSize = 1.4f, colorNoiseRatio = 0.25f),
                compositing = CompositingConfig(vignetteIntensity = 0.40f, vignetteSmoothness = 0.75f, borderMaskAsset = "presets/borders/half_frame_divider.png", enableDateStamp = true)
            )

            "d_slide" -> CameraPreset(
                id = "d_slide",
                displayName = "D Slide",
                subtitle = "Color Reversal Slide • Punchy Velvia Pop",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.07f,
                    tintShift = 0.03f
                ),
                aberration = AberrationConfig(strength = 0.003f, cubicDistortion = -0.004f, falloffPower = 2.5f),
                halation = HalationConfig(enabled = true, threshold = 0.82f, knee = 0.12f, blurRadius = 1.8f, tintRgb = listOf(1.0f, 0.45f, 0.10f), strength = 0.30f),
                grain = GrainConfig(intensity = 0.06f, particleSize = 1.0f, colorNoiseRatio = 0.15f),
                compositing = CompositingConfig(vignetteIntensity = 0.55f, vignetteSmoothness = 0.70f, enableDateStamp = false)
            )

            "ofm_r" -> baseMap["cinestill_800t"]?.copy(aspectRatio = camera.aspectRatio) ?: CameraPreset(
                id = "ofm_r",
                displayName = "OFM R",
                subtitle = "500T Movie Film • Red Halation",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig("presets/luts/cinestill_800t.raw3d", defaultIntensity = 1.0f, temperatureShift = -0.08f, tintShift = 0.02f),
                aberration = AberrationConfig(0.005f, -0.010f, 2.2f),
                halation = HalationConfig(true, 0.72f, 0.16f, 3.2f, listOf(1.0f, 0.20f, 0.05f), 0.85f),
                grain = GrainConfig(0.15f, 1.5f, 0.30f),
                compositing = CompositingConfig(0.65f, 0.75f, emptyList(), null, true)
            )

            // =================================================================
            // VINTAGE 120
            // =================================================================
            "s_classic" -> CameraPreset(
                id = "s_classic",
                displayName = "S Classic",
                subtitle = "Rolleiflex 2.8F TLR • Exquisite Square",
                category = PresetCategory.VINTAGE_120,
                aspectRatio = AspectRatioProfile.RATIO_1_1,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.03f,
                    tintShift = -0.01f
                ),
                aberration = AberrationConfig(strength = 0.002f, cubicDistortion = -0.003f, falloffPower = 2.6f),
                halation = HalationConfig(enabled = true, threshold = 0.85f, knee = 0.10f, blurRadius = 1.6f, tintRgb = listOf(1.0f, 0.40f, 0.15f), strength = 0.25f),
                grain = GrainConfig(intensity = 0.05f, particleSize = 1.1f, colorNoiseRatio = 0.15f),
                compositing = CompositingConfig(vignetteIntensity = 0.50f, vignetteSmoothness = 0.75f, borderMaskAsset = "presets/borders/square_120.png", enableDateStamp = false)
            )

            "hoga" -> CameraPreset(
                id = "hoga",
                displayName = "HOGA",
                subtitle = "Cult 120 Toy Camera • Extreme Vignette & Blur",
                category = PresetCategory.VINTAGE_120,
                aspectRatio = AspectRatioProfile.RATIO_1_1,
                lut = LutConfig(
                    assetPath = "presets/luts/holga_120.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.03f,
                    tintShift = 0.02f
                ),
                aberration = AberrationConfig(strength = 0.022f, cubicDistortion = -0.060f, falloffPower = 1.6f),
                halation = HalationConfig(enabled = false),
                grain = GrainConfig(intensity = 0.16f, particleSize = 2.0f, colorNoiseRatio = 0.40f),
                compositing = CompositingConfig(vignetteIntensity = 1.45f, vignetteSmoothness = 0.35f, lightLeakPool = listOf("presets/leaks/leak_side_burn.png"), borderMaskAsset = "presets/borders/square_120.png", enableDateStamp = false)
            )

            "s_67" -> CameraPreset(
                id = "s_67",
                displayName = "S 67",
                subtitle = "Pentax 67 105mm • Legendary Portrait Bokeh",
                category = PresetCategory.VINTAGE_120,
                aspectRatio = AspectRatioProfile.RATIO_6_7,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.05f,
                    tintShift = 0.0f
                ),
                aberration = AberrationConfig(strength = 0.002f, cubicDistortion = -0.003f, falloffPower = 2.8f),
                halation = HalationConfig(enabled = true, threshold = 0.84f, knee = 0.10f, blurRadius = 1.5f, tintRgb = listOf(1.0f, 0.38f, 0.12f), strength = 0.30f),
                grain = GrainConfig(intensity = 0.04f, particleSize = 1.0f, colorNoiseRatio = 0.15f),
                compositing = CompositingConfig(vignetteIntensity = 0.45f, vignetteSmoothness = 0.80f, enableDateStamp = false)
            )

            "kv80_r" -> CameraPreset(
                id = "kv80_r",
                displayName = "KV80 R",
                subtitle = "Hasselblad Planar 80mm • Studio Fidelity",
                category = PresetCategory.VINTAGE_120,
                aspectRatio = AspectRatioProfile.RATIO_1_1,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.02f,
                    tintShift = 0.0f
                ),
                aberration = AberrationConfig(strength = 0.001f, cubicDistortion = -0.002f, falloffPower = 2.8f),
                halation = HalationConfig(enabled = true, threshold = 0.86f, knee = 0.10f, blurRadius = 1.4f, tintRgb = listOf(1.0f, 0.35f, 0.15f), strength = 0.20f),
                grain = GrainConfig(intensity = 0.04f, particleSize = 1.0f, colorNoiseRatio = 0.12f),
                compositing = CompositingConfig(vignetteIntensity = 0.40f, vignetteSmoothness = 0.80f, borderMaskAsset = "presets/borders/square_120.png", enableDateStamp = false)
            )

            // =================================================================
            // INST COLLECTION
            // =================================================================
            "inst_c" -> CameraPreset(
                id = "inst_c",
                displayName = "Inst C",
                subtitle = "Contemporary Instant • White Border",
                category = PresetCategory.INSTANT,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/polaroid_sx70.raw3d",
                    defaultIntensity = 0.95f,
                    temperatureShift = 0.03f,
                    tintShift = 0.02f
                ),
                aberration = AberrationConfig(strength = 0.006f, cubicDistortion = -0.012f, falloffPower = 2.0f),
                halation = HalationConfig(enabled = true, threshold = 0.78f, knee = 0.14f, blurRadius = 2.0f, tintRgb = listOf(1.0f, 0.40f, 0.20f), strength = 0.40f),
                grain = GrainConfig(intensity = 0.14f, particleSize = 1.5f, colorNoiseRatio = 0.35f),
                compositing = CompositingConfig(vignetteIntensity = 0.65f, vignetteSmoothness = 0.70f, borderMaskAsset = "presets/borders/polaroid_frame.png", enableDateStamp = false)
            )

            "inst_sq" -> CameraPreset(
                id = "inst_sq",
                displayName = "Inst SQ",
                subtitle = "Square Instant • Sharp Chemistry",
                category = PresetCategory.INSTANT,
                aspectRatio = AspectRatioProfile.RATIO_1_1,
                lut = LutConfig(
                    assetPath = "presets/luts/polaroid_sx70.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = -0.01f,
                    tintShift = 0.04f
                ),
                aberration = AberrationConfig(strength = 0.006f, cubicDistortion = -0.012f, falloffPower = 2.0f),
                halation = HalationConfig(enabled = true, threshold = 0.80f, knee = 0.12f, blurRadius = 2.0f, tintRgb = listOf(1.0f, 0.35f, 0.15f), strength = 0.35f),
                grain = GrainConfig(intensity = 0.15f, particleSize = 1.6f, colorNoiseRatio = 0.40f),
                compositing = CompositingConfig(vignetteIntensity = 0.65f, vignetteSmoothness = 0.70f, borderMaskAsset = "presets/borders/square_120.png", enableDateStamp = false)
            )

            "inst_sqc" -> CameraPreset(
                id = "inst_sqc",
                displayName = "Inst SQC",
                subtitle = "Amber Gel Filter • Cozy Sunset Mood",
                category = PresetCategory.INSTANT,
                aspectRatio = AspectRatioProfile.RATIO_1_1,
                lut = LutConfig(
                    assetPath = "presets/luts/polaroid_sx70.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.15f,
                    tintShift = 0.06f
                ),
                aberration = AberrationConfig(strength = 0.007f, cubicDistortion = -0.014f, falloffPower = 2.0f),
                halation = HalationConfig(enabled = true, threshold = 0.76f, knee = 0.15f, blurRadius = 2.4f, tintRgb = listOf(1.0f, 0.50f, 0.10f), strength = 0.50f),
                grain = GrainConfig(intensity = 0.16f, particleSize = 1.7f, colorNoiseRatio = 0.40f),
                compositing = CompositingConfig(vignetteIntensity = 0.70f, vignetteSmoothness = 0.65f, borderMaskAsset = "presets/borders/square_120.png", enableDateStamp = false)
            )

            "paf_r" -> CameraPreset(
                id = "paf_r",
                displayName = "PAF R",
                subtitle = "Instant Polaroid Film • Warm Tones & White Frame",
                category = PresetCategory.INSTANT,
                aspectRatio = AspectRatioProfile.RATIO_1_1,
                lut = LutConfig(
                    assetPath = "presets/luts/polaroid_sx70.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.02f,
                    tintShift = 0.02f
                ),
                aberration = AberrationConfig(strength = 0.007f, cubicDistortion = -0.015f, falloffPower = 2.0f),
                halation = HalationConfig(enabled = true, threshold = 0.80f, knee = 0.12f, blurRadius = 2.2f, tintRgb = listOf(1.0f, 0.35f, 0.15f), strength = 0.35f),
                grain = GrainConfig(intensity = 0.10f, particleSize = 1.6f, colorNoiseRatio = 0.35f),
                compositing = CompositingConfig(vignetteIntensity = 0.40f, vignetteSmoothness = 0.65f, borderMaskAsset = "presets/borders/polaroid_frame.png", enableDateStamp = false)
            )

            // =================================================================
            // LEGACY
            // =================================================================
            "d_exp" -> CameraPreset(
                id = "d_exp",
                displayName = "D Exp",
                subtitle = "Experimental Profile • Moody Teal Grain",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/fuji_superia_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = -0.10f,
                    tintShift = 0.08f
                ),
                aberration = AberrationConfig(strength = 0.010f, cubicDistortion = -0.015f, falloffPower = 2.0f),
                halation = HalationConfig(enabled = true, threshold = 0.72f, knee = 0.16f, blurRadius = 2.8f, tintRgb = listOf(0.4f, 0.9f, 1.0f), strength = 0.60f),
                grain = GrainConfig(intensity = 0.22f, particleSize = 1.9f, colorNoiseRatio = 0.35f),
                compositing = CompositingConfig(vignetteIntensity = 0.80f, vignetteSmoothness = 0.60f, enableDateStamp = true)
            )

            "nt16" -> CameraPreset(
                id = "nt16",
                displayName = "NT16",
                subtitle = "Miniature Spy Camera • Micro Lens Vignette",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/contax_t2.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = -0.04f,
                    tintShift = -0.02f
                ),
                aberration = AberrationConfig(strength = 0.012f, cubicDistortion = -0.020f, falloffPower = 2.1f),
                halation = HalationConfig(enabled = false),
                grain = GrainConfig(intensity = 0.20f, particleSize = 1.6f, colorNoiseRatio = 0.25f),
                compositing = CompositingConfig(vignetteIntensity = 0.85f, vignetteSmoothness = 0.55f, enableDateStamp = false)
            )

            "inst_ss" -> CameraPreset(
                id = "inst_ss",
                displayName = "Inst SS",
                subtitle = "Instant Stop-Motion • Punchy Pop",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/polaroid_sx70.raw3d",
                    defaultIntensity = 0.90f,
                    temperatureShift = 0.04f,
                    tintShift = 0.03f
                ),
                aberration = AberrationConfig(strength = 0.005f, cubicDistortion = -0.008f, falloffPower = 2.2f),
                halation = HalationConfig(enabled = true, threshold = 0.80f, strength = 0.30f),
                grain = GrainConfig(intensity = 0.14f, particleSize = 1.4f, colorNoiseRatio = 0.30f),
                compositing = CompositingConfig(vignetteIntensity = 0.60f, vignetteSmoothness = 0.70f, enableDateStamp = false)
            )

            "puli" -> CameraPreset(
                id = "puli",
                displayName = "Puli",
                subtitle = "Retro 4-Strip Photobooth Flash",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/ccd_powershot.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = 0.05f,
                    tintShift = 0.02f
                ),
                aberration = AberrationConfig(strength = 0.004f, cubicDistortion = -0.006f, falloffPower = 2.3f),
                halation = HalationConfig(enabled = true, threshold = 0.82f, strength = 0.30f),
                grain = GrainConfig(intensity = 0.14f, particleSize = 1.3f, colorNoiseRatio = 0.25f),
                compositing = CompositingConfig(vignetteIntensity = 0.50f, vignetteSmoothness = 0.75f, enableDateStamp = true)
            )

            "kv88" -> CameraPreset(
                id = "kv88",
                displayName = "KV88",
                subtitle = "Vintage Studio Medium Format Curve",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = AspectRatioProfile.RATIO_1_1,
                lut = LutConfig(
                    assetPath = "presets/luts/portra_400.raw3d",
                    defaultIntensity = 0.90f,
                    temperatureShift = 0.08f,
                    tintShift = -0.03f
                ),
                aberration = AberrationConfig(strength = 0.003f, cubicDistortion = -0.005f, falloffPower = 2.5f),
                halation = HalationConfig(enabled = true, threshold = 0.82f, knee = 0.12f, blurRadius = 1.8f, tintRgb = listOf(1.0f, 0.40f, 0.15f), strength = 0.30f),
                grain = GrainConfig(intensity = 0.08f, particleSize = 1.2f, colorNoiseRatio = 0.18f),
                compositing = CompositingConfig(vignetteIntensity = 0.55f, vignetteSmoothness = 0.75f, borderMaskAsset = "presets/borders/square_120.png", enableDateStamp = false)
            )

            "135_sr" -> CameraPreset(
                id = "135_sr",
                displayName = "135 SR",
                subtitle = "Weather-Sealed Action • Vivid Primary Color",
                category = PresetCategory.VINTAGE_135,
                aspectRatio = camera.aspectRatio,
                lut = LutConfig(
                    assetPath = "presets/luts/fuji_superia_400.raw3d",
                    defaultIntensity = 1.0f,
                    temperatureShift = -0.06f,
                    tintShift = -0.08f
                ),
                aberration = AberrationConfig(strength = 0.008f, cubicDistortion = -0.012f, falloffPower = 2.2f),
                halation = HalationConfig(enabled = false),
                grain = GrainConfig(intensity = 0.12f, particleSize = 1.3f, colorNoiseRatio = 0.25f),
                compositing = CompositingConfig(vignetteIntensity = 0.55f, vignetteSmoothness = 0.75f, enableDateStamp = false)
            )

            // Default fallback
            else -> baseMap[camera.underlyingPresetId]?.copy(aspectRatio = camera.aspectRatio)
                ?: basePresets.firstOrNull()
                ?: CameraPreset(
                    id = camera.id,
                    displayName = camera.name,
                    subtitle = camera.description,
                    category = PresetCategory.VINTAGE_135,
                    aspectRatio = camera.aspectRatio,
                    lut = LutConfig("presets/luts/portra_400.raw3d"),
                    aberration = AberrationConfig(),
                    halation = HalationConfig(),
                    grain = GrainConfig(),
                    compositing = CompositingConfig()
                )
        }
    }
}
