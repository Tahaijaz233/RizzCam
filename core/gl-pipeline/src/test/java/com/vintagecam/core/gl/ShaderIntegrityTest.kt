package com.vintagecam.core.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit test suite verifying the mathematical, syntactical, and uniform declaration integrity
 * of all 13 OpenGL ES 3.0 shaders in RizzCam.
 *
 * Validates Section 1.3 GLSL ES 3.0 Shader Pipeline:
 * - `#version 300 es` header presence in all vertex and fragment shaders.
 * - Precision qualifiers.
 * - Expected uniform and sampler bindings for all 6 pipeline passes + optical accessories.
 * - Entrypoint validity and balanced brace syntax.
 */
class ShaderIntegrityTest {

    private lateinit var shadersDir: File

    private val allShaderFilenames = listOf(
        "fullscreen_quad.vert",
        "oes_ingest.frag",
        "standard_2d_ingest.frag",
        "lut_filter.frag",
        "chromatic_aberration.frag",
        "accessory_prism.frag",
        "accessory_star.frag",
        "accessory_fisheye.frag",
        "halation_threshold.frag",
        "separable_blur.frag",
        "halation_composite.frag",
        "film_grain.frag",
        "compositing.frag"
    )

    @Before
    fun setUp() {
        val possiblePaths = listOf(
            File("src/main/assets/shaders"),
            File("core/gl-pipeline/src/main/assets/shaders")
        )

        shadersDir = possiblePaths.firstOrNull { it.exists() && it.isDirectory }
            ?: throw IllegalStateException("Could not locate shaders directory in: $possiblePaths")
    }

    @Test
    fun testAllThirteenShadersExist() {
        for (filename in allShaderFilenames) {
            val file = File(shadersDir, filename)
            assertTrue("Shader file $filename must exist", file.exists() && file.isFile)
            assertTrue("Shader file $filename must not be empty", file.length() > 0)
        }
    }

    @Test
    fun testShaderHeadersDeclareVersion300Es() {
        for (filename in allShaderFilenames) {
            val file = File(shadersDir, filename)
            val lines = file.readLines()

            // Find first non-empty, non-comment line or check line 1
            val firstDirective = lines
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() && !it.startsWith("//") && !it.startsWith("/*") }

            assertNotNull("Shader $filename must contain content", firstDirective)
            assertEquals(
                "Shader $filename must declare #version 300 es as first directive",
                "#version 300 es",
                firstDirective
            )
        }
    }

    @Test
    fun testFragmentShadersDeclarePrecision() {
        val fragShaders = allShaderFilenames.filter { it.endsWith(".frag") }

        for (filename in fragShaders) {
            val content = File(shadersDir, filename).readText()
            val hasPrecision = content.contains("precision mediump float;") ||
                    content.contains("precision highp float;") ||
                    content.contains("precision lowp float;")
            assertTrue("Fragment shader $filename must declare float precision", hasPrecision)
        }
    }

    @Test
    fun testVertexShaderUniformsAndOutputs() {
        val vertShader = File(shadersDir, "fullscreen_quad.vert").readText()
        assertTrue("Vertex shader must declare uTexMatrix", vertShader.contains("uTexMatrix"))
        assertTrue("Vertex shader must declare vTexCoord out", vertShader.contains("vTexCoord"))
        assertTrue("Vertex shader must compute gl_Position", vertShader.contains("gl_Position"))
    }

    @Test
    fun testKeyUniformDeclarationsAcrossPipeline() {
        // Pass 0 OES Ingest
        val oesIngest = File(shadersDir, "oes_ingest.frag").readText()
        assertTrue(oesIngest.contains("samplerExternalOES"))
        assertTrue(oesIngest.contains("uExposureEv"))
        assertTrue(oesIngest.contains("uTemperatureShift"))
        assertTrue(oesIngest.contains("uTintShift"))

        // Pass 0 2D Still Ingest
        val stillIngest = File(shadersDir, "standard_2d_ingest.frag").readText()
        assertTrue(stillIngest.contains("sampler2D"))
        assertTrue(stillIngest.contains("uExposureEv"))

        // Pass 1 3D LUT
        val lutFilter = File(shadersDir, "lut_filter.frag").readText()
        assertTrue(lutFilter.contains("sampler3D"))
        assertTrue(lutFilter.contains("uLutIntensity"))

        // Pass 2 Chromatic Aberration
        val aberration = File(shadersDir, "chromatic_aberration.frag").readText()
        assertTrue(aberration.contains("uAberrationStrength"))
        assertTrue(aberration.contains("uCubicDistortion"))

        // Pass 2B Optical Accessories
        val prism = File(shadersDir, "accessory_prism.frag").readText()
        assertTrue(prism.contains("uFacetCount"))

        val star = File(shadersDir, "accessory_star.frag").readText()
        assertTrue(star.contains("uStarIntensity"))

        val fisheye = File(shadersDir, "accessory_fisheye.frag").readText()
        assertTrue(fisheye.contains("uFisheyeCurvature"))

        // Pass 3 Halation
        val threshold = File(shadersDir, "halation_threshold.frag").readText()
        assertTrue(threshold.contains("uThreshold"))

        val blur = File(shadersDir, "separable_blur.frag").readText()
        assertTrue(blur.contains("uTexelOffset"))

        val halationComp = File(shadersDir, "halation_composite.frag").readText()
        assertTrue(halationComp.contains("uBaseTexture"))
        assertTrue(halationComp.contains("uHalationBlurTexture"))

        // Pass 4 Film Grain
        val grain = File(shadersDir, "film_grain.frag").readText()
        assertTrue(grain.contains("uGrainIntensity"))
        assertTrue(grain.contains("uTimeSeed"))

        // Pass 5 Final Compositing
        val compositing = File(shadersDir, "compositing.frag").readText()
        assertTrue(compositing.contains("uVignetteIntensity"))
    }

    @Test
    fun testShaderEntrypointsAndBraces() {
        for (filename in allShaderFilenames) {
            val content = File(shadersDir, filename).readText()
            assertTrue("Shader $filename must contain void main()", content.contains("void main()"))

            var openCount = 0
            var closeCount = 0
            for (ch in content) {
                if (ch == '{') openCount++
                if (ch == '}') closeCount++
            }
            assertEquals("Shader $filename must have balanced braces", openCount, closeCount)
            assertTrue("Shader $filename must have at least one body brace", openCount > 0)
        }
    }
}
