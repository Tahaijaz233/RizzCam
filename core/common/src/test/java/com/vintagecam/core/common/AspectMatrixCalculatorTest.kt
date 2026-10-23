package com.vintagecam.core.common

import com.vintagecam.core.common.math.AspectMatrixCalculator
import com.vintagecam.domain.presets.model.AspectRatioProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying zero-stutter aspect ratio cropping matrix mathematics.
 *
 * Validates Section 1.2 Aspect-Ratio & Crop Mathematics:
 * Center-crops native 4:3 sensor readouts into 3:2, 1:1, 6:7, and 3:4 half-frame diptych.
 */
class AspectMatrixCalculatorTest {

    private lateinit var calculator: AspectMatrixCalculator
    private val identityMatrix = FloatArray(16)
    private val outMatrix = FloatArray(16)

    @Before
    fun setUp() {
        calculator = AspectMatrixCalculator()
        AspectMatrixCalculator.setIdentityMatrix(identityMatrix)
    }

    @Test
    fun testNativeRatio43IdentityCrop() {
        // Native 4:3 sensor (e.g. 4000x3000) mapped to 4:3 profile -> no cropping needed
        val factors = calculator.computeCropFactors(4000, 3000, AspectRatioProfile.RATIO_4_3)
        assertEquals(1.0f, factors.sx, 1e-5f)
        assertEquals(1.0f, factors.sy, 1e-5f)
        assertEquals(0.0f, factors.tx, 1e-5f)
        assertEquals(0.0f, factors.ty, 1e-5f)

        calculator.calculate(identityMatrix, 4000, 3000, AspectRatioProfile.RATIO_4_3, outMatrix)
        assertEquals(1.0f, outMatrix[0], 1e-5f)  // Sx
        assertEquals(1.0f, outMatrix[5], 1e-5f)  // Sy
        assertEquals(0.0f, outMatrix[12], 1e-5f) // Tx
        assertEquals(0.0f, outMatrix[13], 1e-5f) // Ty
    }

    @Test
    fun testRatio32CropFrom43Sensor() {
        // 4000x3000 (aspect 4/3) -> 3:2 (aspect 1.5). dst > src, so vertical crop
        // Sy = (4/3) / (3/2) = 8/9 ≈ 0.8888889f
        // Ty = (1 - 8/9) / 2 = 1/18 ≈ 0.055555556f
        val factors = calculator.computeCropFactors(4000, 3000, AspectRatioProfile.RATIO_3_2)
        assertEquals(1.0f, factors.sx, 1e-5f)
        assertEquals(8f / 9f, factors.sy, 1e-5f)
        assertEquals(0.0f, factors.tx, 1e-5f)
        assertEquals(1f / 18f, factors.ty, 1e-5f)

        calculator.calculate(identityMatrix, 4000, 3000, AspectRatioProfile.RATIO_3_2, outMatrix)
        assertEquals(1.0f, outMatrix[0], 1e-5f)
        assertEquals(8f / 9f, outMatrix[5], 1e-5f)
        assertEquals(0.0f, outMatrix[12], 1e-5f)
        assertEquals(1f / 18f, outMatrix[13], 1e-5f)
    }

    @Test
    fun testRatio11SquareCropFrom43Sensor() {
        // 4000x3000 (aspect 4/3) -> 1:1 (aspect 1.0). dst < src, so horizontal crop
        // Sx = 1.0 / (4/3) = 3/4 = 0.75f
        // Tx = (1 - 0.75) / 2 = 0.125f
        val factors = calculator.computeCropFactors(4000, 3000, AspectRatioProfile.RATIO_1_1)
        assertEquals(0.75f, factors.sx, 1e-5f)
        assertEquals(1.0f, factors.sy, 1e-5f)
        assertEquals(0.125f, factors.tx, 1e-5f)
        assertEquals(0.0f, factors.ty, 1e-5f)

        calculator.calculate(identityMatrix, 4000, 3000, AspectRatioProfile.RATIO_1_1, outMatrix)
        assertEquals(0.75f, outMatrix[0], 1e-5f)
        assertEquals(1.0f, outMatrix[5], 1e-5f)
        assertEquals(0.125f, outMatrix[12], 1e-5f)
        assertEquals(0.0f, outMatrix[13], 1e-5f)
    }

    @Test
    fun testRatio67CropFrom43Sensor() {
        // 4000x3000 (aspect 4/3) -> 6:7 (aspect 6/7). dst < src, so horizontal crop
        // Sx = (6/7) / (4/3) = 18/28 = 9/14 ≈ 0.64285713f
        val factors = calculator.computeCropFactors(4000, 3000, AspectRatioProfile.RATIO_6_7)
        assertEquals(9f / 14f, factors.sx, 1e-5f)
        assertEquals(1.0f, factors.sy, 1e-5f)
        assertEquals((1f - 9f / 14f) * 0.5f, factors.tx, 1e-5f)
        assertEquals(0.0f, factors.ty, 1e-5f)
    }

    @Test
    fun testHalfFrameDiptychCropFrom43Sensor() {
        // Olympus PEN Half-Frame: captures 3:4 portrait on 4:3 native sensor
        // dst = 3/4 = 0.75. src = 4/3. dst < src, so horizontal crop
        // Sx = (3/4) / (4/3) = 9/16 = 0.5625f
        // Tx = (1 - 0.5625) / 2 = 0.21875f
        val factors = calculator.computeCropFactors(4000, 3000, AspectRatioProfile.HALF_FRAME_DIPTYCH)
        assertEquals(9f / 16f, factors.sx, 1e-5f)
        assertEquals(1.0f, factors.sy, 1e-5f)
        assertEquals(0.21875f, factors.tx, 1e-5f)
        assertEquals(0.0f, factors.ty, 1e-5f)

        calculator.calculate(identityMatrix, 4000, 3000, AspectRatioProfile.HALF_FRAME_DIPTYCH, outMatrix)
        assertEquals(0.5625f, outMatrix[0], 1e-5f)
        assertEquals(1.0f, outMatrix[5], 1e-5f)
        assertEquals(0.21875f, outMatrix[12], 1e-5f)
        assertEquals(0.0f, outMatrix[13], 1e-5f)
    }

    @Test
    fun testCenterPointInvariance() {
        // Center of UV coordinate space (0.5, 0.5) must map back to (0.5, 0.5) after center-cropping
        val profiles = listOf(
            AspectRatioProfile.RATIO_4_3,
            AspectRatioProfile.RATIO_3_2,
            AspectRatioProfile.RATIO_1_1,
            AspectRatioProfile.RATIO_6_7,
            AspectRatioProfile.HALF_FRAME_DIPTYCH
        )

        for (profile in profiles) {
            val factors = calculator.computeCropFactors(4000, 3000, profile)
            val mappedU = 0.5f * factors.sx + factors.tx
            val mappedV = 0.5f * factors.sy + factors.ty
            assertEquals("Center U must be 0.5 for $profile", 0.5f, mappedU, 1e-5f)
            assertEquals("Center V must be 0.5 for $profile", 0.5f, mappedV, 1e-5f)
        }
    }

    @Test
    fun testUvBoundsEnclosed() {
        // All transformed UVs within [0, 1] must map to valid sub-regions within [0, 1]
        val profiles = listOf(
            AspectRatioProfile.RATIO_4_3,
            AspectRatioProfile.RATIO_3_2,
            AspectRatioProfile.RATIO_1_1,
            AspectRatioProfile.RATIO_6_7,
            AspectRatioProfile.HALF_FRAME_DIPTYCH
        )

        for (profile in profiles) {
            val factors = calculator.computeCropFactors(4000, 3000, profile)
            val minU = 0.0f * factors.sx + factors.tx
            val maxU = 1.0f * factors.sx + factors.tx
            val minV = 0.0f * factors.sy + factors.ty
            val maxV = 1.0f * factors.sy + factors.ty

            assertTrue("minU >= 0 for $profile", minU >= 0.0f)
            assertTrue("maxU <= 1 for $profile", maxU <= 1.0f)
            assertTrue("minV >= 0 for $profile", minV >= 0.0f)
            assertTrue("maxV <= 1 for $profile", maxV <= 1.0f)
            assertTrue("minU <= maxU for $profile", minU <= maxU)
            assertTrue("minV <= maxV for $profile", minV <= maxV)
        }
    }
}
