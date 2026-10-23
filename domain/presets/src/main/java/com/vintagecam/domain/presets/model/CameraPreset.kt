package com.vintagecam.domain.presets.model

import kotlinx.serialization.Serializable

@Serializable
enum class AspectRatioProfile(val widthRatio: Float, val heightRatio: Float) {
    RATIO_4_3(4f, 3f),
    RATIO_3_2(3f, 2f),
    RATIO_1_1(1f, 1f),
    RATIO_6_7(6f, 7f),
    HALF_FRAME_DIPTYCH(3f, 4f); // Captures two 3:4 frames stitched onto a 3:2 landscape canvas

    val aspectRatio: Float
        get() = widthRatio / heightRatio
}

@Serializable
data class LutConfig(
    val assetPath: String,        // e.g., "presets/luts/portra_400.raw3d"
    val lutSize: Int = 64,
    val isHaldClut: Boolean = false,
    val defaultIntensity: Float = 1.0f,
    val temperatureShift: Float = 0.0f,
    val tintShift: Float = 0.0f
)

@Serializable
data class AberrationConfig(
    val strength: Float = 0.004f,
    val cubicDistortion: Float = -0.012f,
    val falloffPower: Float = 2.2f
)

@Serializable
data class HalationConfig(
    val enabled: Boolean = false,
    val threshold: Float = 0.80f,
    val knee: Float = 0.14f,
    val blurRadius: Float = 2.2f,
    val tintRgb: List<Float> = listOf(1.0f, 0.26f, 0.08f),
    val strength: Float = 0.45f
)

@Serializable
data class GrainConfig(
    val intensity: Float = 0.12f,
    val particleSize: Float = 1.35f,
    val colorNoiseRatio: Float = 0.25f
)

@Serializable
data class CompositingConfig(
    val vignetteIntensity: Float = 0.65f,
    val vignetteSmoothness: Float = 0.70f,
    val lightLeakPool: List<String> = emptyList(),
    val borderMaskAsset: String? = null,
    val enableDateStamp: Boolean = false
)

@Serializable
data class CameraPreset(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val category: PresetCategory = PresetCategory.VINTAGE_135,
    val aspectRatio: AspectRatioProfile,
    val lut: LutConfig,
    val aberration: AberrationConfig,
    val halation: HalationConfig,
    val grain: GrainConfig,
    val compositing: CompositingConfig
)
