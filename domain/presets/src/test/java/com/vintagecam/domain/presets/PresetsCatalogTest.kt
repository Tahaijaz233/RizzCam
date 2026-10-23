package com.vintagecam.domain.presets

import com.vintagecam.domain.presets.model.CameraPreset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit test suite verifying the integrity, serialization, and asset validity of the presets catalog.
 *
 * Validates Section 2.1 Presets Catalog & Domain Model:
 * - JSON schema validation of `presets_catalog.json`.
 * - Exactly 8 signature camera profiles.
 * - Authenticity of 64^3 binary .raw3d 3D LUT assets (exact 786,432 bytes).
 * - Film border masks and light leak assets.
 */
class PresetsCatalogTest {

    private lateinit var json: Json
    private lateinit var catalogFile: File
    private lateinit var assetsDir: File
    private lateinit var presets: List<CameraPreset>

    @Before
    fun setUp() {
        json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        // Support execution from both module dir and workspace root
        val possiblePaths = listOf(
            File("src/main/assets"),
            File("domain/presets/src/main/assets")
        )

        assetsDir = possiblePaths.firstOrNull { it.exists() && it.isDirectory }
            ?: throw IllegalStateException("Could not locate assets directory in: $possiblePaths")

        catalogFile = File(assetsDir, "presets/presets_catalog.json")
        assertTrue("Catalog file must exist at ${catalogFile.absolutePath}", catalogFile.exists())

        val jsonString = catalogFile.readText()
        presets = json.decodeFromString<List<CameraPreset>>(jsonString)
    }

    @Test
    fun testPresetsCatalogCount() {
        assertEquals("Catalog must contain exactly 8 signature presets", 8, presets.size)
    }

    @Test
    fun testExpectedPresetIds() {
        val expectedIds = setOf(
            "portra_400_35mm",
            "fuji_superia_half",
            "ccd_power_shot",
            "contax_t2_party",
            "cinestill_800t",
            "holga_120n",
            "polaroid_sx70",
            "aerochrome_ir"
        )

        val actualIds = presets.map { it.id }.toSet()
        assertEquals("All expected signature preset IDs must be present", expectedIds, actualIds)
    }

    @Test
    fun testAllLutFilesExistAndAre786432Bytes() {
        val expectedByteSize = 64 * 64 * 64 * 3 // 786,432 bytes

        for (preset in presets) {
            val lutConfig = preset.lut
            assertNotNull("Preset ${preset.id} must define a lut config", lutConfig)

            val lutFile = File(assetsDir, lutConfig.assetPath)
            assertTrue(
                "LUT file for ${preset.id} must exist at ${lutFile.path}",
                lutFile.exists() && lutFile.isFile
            )
            assertEquals(
                "LUT binary ${lutConfig.assetPath} must be exactly 786,432 bytes (64^3 RGB8)",
                expectedByteSize.toLong(),
                lutFile.length()
            )
            assertEquals("LUT size must be 64", 64, lutConfig.lutSize)
        }
    }

    @Test
    fun testBorderMaskAndLightLeakAssetsExist() {
        for (preset in presets) {
            val compositing = preset.compositing

            compositing.borderMaskAsset?.let { borderPath ->
                val borderFile = File(assetsDir, borderPath)
                assertTrue(
                    "Border mask $borderPath for ${preset.id} must exist",
                    borderFile.exists() && borderFile.isFile
                )
            }

            for (leakPath in compositing.lightLeakPool) {
                val leakFile = File(assetsDir, leakPath)
                assertTrue(
                    "Light leak $leakPath for ${preset.id} must exist",
                    leakFile.exists() && leakFile.isFile
                )
            }
        }
    }

    @Test
    fun testParameterBounds() {
        for (preset in presets) {
            assertTrue("Vignette intensity must be non-negative for ${preset.id}",
                preset.compositing.vignetteIntensity >= 0.0f)
            assertTrue("Vignette smoothness must be in [0, 1] for ${preset.id}",
                preset.compositing.vignetteSmoothness in 0.0f..1.0f)
            assertTrue("Grain intensity must be positive for ${preset.id}",
                preset.grain.intensity >= 0.0f)

            if (preset.halation.enabled) {
                assertTrue("Halation threshold must be in [0, 1] for ${preset.id}",
                    preset.halation.threshold in 0.0f..1.0f)
                assertTrue("Halation strength must be positive for ${preset.id}",
                    preset.halation.strength >= 0.0f)
                assertEquals("Halation tint RGB must have 3 components",
                    3, preset.halation.tintRgb.size)
            }
        }
    }
}
