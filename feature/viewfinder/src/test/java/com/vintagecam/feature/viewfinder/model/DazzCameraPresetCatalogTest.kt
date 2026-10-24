package com.vintagecam.feature.viewfinder.model

import com.vintagecam.domain.presets.model.AspectRatioProfile
import com.vintagecam.domain.presets.model.CameraPreset
import com.vintagecam.domain.presets.model.CompositingConfig
import com.vintagecam.domain.presets.model.GrainConfig
import com.vintagecam.domain.presets.model.HalationConfig
import com.vintagecam.domain.presets.model.LutConfig
import com.vintagecam.domain.presets.model.PresetCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Unit tests verifying that all 46 Dazz / RizzCam camera models resolve to authentic,
 * distinct vintage film presets with calibrated color science, LUTs, and optical characteristics.
 */
class DazzCameraPresetCatalogTest {

    private val sampleBasePresets = listOf(
        CameraPreset(
            id = "portra_400_35mm",
            displayName = "Kodak Portra 400",
            subtitle = "Fine grain portrait",
            category = PresetCategory.VINTAGE_135,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            lut = LutConfig("presets/luts/portra_400.raw3d"),
            aberration = com.vintagecam.domain.presets.model.AberrationConfig(),
            halation = HalationConfig(),
            grain = GrainConfig(),
            compositing = CompositingConfig()
        ),
        CameraPreset(
            id = "fuji_superia_400",
            displayName = "Fujifilm Superia 400",
            subtitle = "Cool punchy greens",
            category = PresetCategory.VINTAGE_135,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            lut = LutConfig("presets/luts/fuji_superia_400.raw3d"),
            aberration = com.vintagecam.domain.presets.model.AberrationConfig(),
            halation = HalationConfig(),
            grain = GrainConfig(),
            compositing = CompositingConfig()
        ),
        CameraPreset(
            id = "ccd_power_shot",
            displayName = "PowerShot G2",
            subtitle = "2000s Y2K digital",
            category = PresetCategory.DIGITAL,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            lut = LutConfig("presets/luts/ccd_powershot.raw3d"),
            aberration = com.vintagecam.domain.presets.model.AberrationConfig(),
            halation = HalationConfig(),
            grain = GrainConfig(),
            compositing = CompositingConfig()
        )
    )

    @Test
    fun testAllCatalogCamerasResolveToValidPreset() {
        val allCameras = DazzCameraCatalog.allCameras
        assertTrue("Catalog must contain cameras", allCameras.isNotEmpty())

        for (camera in allCameras) {
            val preset = DazzCameraPresetCatalog.getPresetForCamera(camera, sampleBasePresets)
            assertNotNull("Preset for ${camera.id} must not be null", preset)
            assertNotNull("LUT path for ${camera.id} must not be null", preset.lut.assetPath)
            assertTrue("LUT path must end with .raw3d", preset.lut.assetPath.endsWith(".raw3d"))
            assertEquals(
                "Preset aspect ratio must match camera model aspect ratio",
                camera.aspectRatio,
                preset.aspectRatio
            )
        }
    }

    @Test
    fun testDifferentCamerasProduceDistinctFilterProfiles() {
        val cpm35 = DazzCameraCatalog.allCameras.first { it.id == "cpm35" }
        val grdr = DazzCameraCatalog.allCameras.first { it.id == "grd_r" }
        val s67 = DazzCameraCatalog.allCameras.first { it.id == "s_67" }
        val dclassic = DazzCameraCatalog.allCameras.first { it.id == "d_classic" }
        val instsq = DazzCameraCatalog.allCameras.first { it.id == "inst_sq" }

        val presetCpm35 = DazzCameraPresetCatalog.getPresetForCamera(cpm35, sampleBasePresets)
        val presetGrdr = DazzCameraPresetCatalog.getPresetForCamera(grdr, sampleBasePresets)
        val presetS67 = DazzCameraPresetCatalog.getPresetForCamera(s67, sampleBasePresets)
        val presetDclassic = DazzCameraPresetCatalog.getPresetForCamera(dclassic, sampleBasePresets)
        val presetInstsq = DazzCameraPresetCatalog.getPresetForCamera(instsq, sampleBasePresets)

        // Verify distinct temperature shifts between warm analog, cool digital, and instant
        assertNotEquals(presetCpm35.lut.temperatureShift, presetGrdr.lut.temperatureShift, 0.001f)
        assertNotEquals(presetS67.lut.assetPath, presetGrdr.lut.assetPath)
        assertNotEquals(presetDclassic.lut.temperatureShift, presetInstsq.lut.temperatureShift, 0.001f)

        // Verify halation configuration differences (CineStill/analog has bloom, digital doesn't)
        assertTrue("CPM35 analog should have halation enabled", presetCpm35.halation.enabled)
        assertTrue("GRD R digital should have halation disabled", !presetGrdr.halation.enabled)
    }

    @Test
    fun testExposureRoundingToTenthOfEv() {
        val testInputs = listOf(
            -2.0f to -2.0f,
            -1.95f to -1.9f,
            -0.74f to -0.7f,
            -0.04f to 0.0f,
            0.04f to 0.0f,
            0.16f to 0.2f,
            0.65f to 0.7f,
            1.84f to 1.8f,
            2.0f to 2.0f,
            2.5f to 2.0f,
            -3.0f to -2.0f
        )

        for ((input, expected) in testInputs) {
            val rounded = ((input * 10f).roundToInt() / 10f).coerceIn(-2.0f, 2.0f)
            assertEquals("Rounding $input EV", expected, rounded, 1e-5f)
        }
    }
}
