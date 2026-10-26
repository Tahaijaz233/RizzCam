package com.vintagecam.core.common

import com.vintagecam.core.common.math.WhiteBalanceMath
import com.vintagecam.core.common.model.WhiteBalanceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WhiteBalanceMathTest {

    @Test
    fun testGoldenKelvinValues() {
        val neutral = WhiteBalanceMath.offsetToKelvin(0f)
        assertEquals("Offset 0 must map to 6500K neutral daylight", 6500f, neutral, 1.0f)

        val coolMax = WhiteBalanceMath.offsetToKelvin(-100f)
        assertEquals("Offset -100 must map to 10000K cool sky", 10000f, coolMax, 1.0f)

        val warmMax = WhiteBalanceMath.offsetToKelvin(100f)
        assertEquals("Offset +100 must map to 2000K warm tungsten/candlelight", 2000f, warmMax, 1.0f)
    }

    @Test
    fun testKelvinToOffsetGoldenValues() {
        val neutralOffset = WhiteBalanceMath.kelvinToOffset(6500f)
        assertEquals("6500K must map to offset 0", 0f, neutralOffset, 0.05f)

        val coolOffset = WhiteBalanceMath.kelvinToOffset(10000f)
        assertEquals("10000K must map to offset -100", -100f, coolOffset, 0.05f)

        val warmOffset = WhiteBalanceMath.kelvinToOffset(2000f)
        assertEquals("2000K must map to offset +100", 100f, warmOffset, 0.05f)
    }

    @Test
    fun testMonotonicity() {
        var prevKelvin = Float.MAX_VALUE
        for (offset in -100..100) {
            val k = WhiteBalanceMath.offsetToKelvin(offset.toFloat())
            assertTrue("Kelvin must strictly decrease as offset increases (mired increases)", k < prevKelvin)
            prevKelvin = k
        }
    }

    @Test
    fun testRoundTripMapping() {
        for (offset in -100..100 step 5) {
            val k = WhiteBalanceMath.offsetToKelvin(offset.toFloat())
            val roundTripOffset = WhiteBalanceMath.kelvinToOffset(k)
            assertEquals("Round trip for offset $offset should match", offset.toFloat(), roundTripOffset, 0.1f)
        }
    }

    @Test
    fun testKelvinToRgbGoldenApproximations() {
        val rgb6500 = WhiteBalanceMath.kelvinToRgb(6500f)
        assertEquals("6500K Red should be ~1.0", 1.0f, rgb6500[0], 0.02f)
        assertEquals("6500K Green should be ~1.0", 1.0f, rgb6500[1], 0.02f)
        assertEquals("6500K Blue should be ~1.0", 1.0f, rgb6500[2], 0.02f)

        val rgb3000 = WhiteBalanceMath.kelvinToRgb(3000f)
        assertEquals("3000K Red should be 1.0", 1.0f, rgb3000[0], 0.02f)
        assertEquals("3000K Green should be ~0.70-0.72", 0.70f, rgb3000[1], 0.03f)
        assertEquals("3000K Blue should be ~0.42", 0.42f, rgb3000[2], 0.03f)
    }

    @Test
    fun testRubberBandPhysics() {
        // Within bounds
        assertEquals(0f, WhiteBalanceMath.rubberBand(0f), 0.001f)
        assertEquals(50f, WhiteBalanceMath.rubberBand(50f), 0.001f)
        assertEquals(-50f, WhiteBalanceMath.rubberBand(-50f), 0.001f)

        // Overshoot past positive boundary (+110)
        val overHigh = WhiteBalanceMath.rubberBand(110f, -100f, 100f, 0.35f)
        assertEquals(103.5f, overHigh, 0.001f)

        // Overshoot past negative boundary (-110)
        val overLow = WhiteBalanceMath.rubberBand(-110f, -100f, 100f, 0.35f)
        assertEquals(-103.5f, overLow, 0.001f)
    }

    @Test
    fun testSignConventionAndFormatSigned() {
        val coolState = WhiteBalanceState(offset = -90f, isAuto = false)
        assertEquals(-9, coolState.offsetDisplay)
        assertTrue(coolState.kelvinLabel > 6500)
        assertEquals("−9", WhiteBalanceMath.formatSigned(coolState.offsetDisplay))

        val warmState = WhiteBalanceState(offset = 70f, isAuto = false)
        assertEquals(7, warmState.offsetDisplay)
        assertTrue(warmState.kelvinLabel < 6500)
        assertEquals("+7", WhiteBalanceMath.formatSigned(warmState.offsetDisplay))

        val neutralState = WhiteBalanceState(offset = 0f, isAuto = true)
        assertEquals(0, neutralState.offsetDisplay)
        assertEquals(6500, neutralState.kelvinLabel)
        assertEquals("0", WhiteBalanceMath.formatSigned(neutralState.offsetDisplay))
    }

    @Test
    fun testOffsetToDisplaySymmetryAndBoundaries() {
        // Neutral daylight
        assertEquals(0, WhiteBalanceMath.offsetToDisplay(0f))
        assertEquals(0, WhiteBalanceMath.offsetToDisplay(4.9f))
        assertEquals(0, WhiteBalanceMath.offsetToDisplay(-4.9f))

        // First transition unit
        assertEquals(1, WhiteBalanceMath.offsetToDisplay(5.0f))
        assertEquals(-1, WhiteBalanceMath.offsetToDisplay(-5.0f))

        // Second transition unit (exact failure case on CI with naive roundToInt)
        assertEquals(2, WhiteBalanceMath.offsetToDisplay(15.0f))
        assertEquals(-2, WhiteBalanceMath.offsetToDisplay(-15.0f))

        // Clamping and overshoot
        assertEquals(10, WhiteBalanceMath.offsetToDisplay(100f))
        assertEquals(-10, WhiteBalanceMath.offsetToDisplay(-100f))
        assertEquals(10, WhiteBalanceMath.offsetToDisplay(120f))
        assertEquals(-10, WhiteBalanceMath.offsetToDisplay(-120f))

        // Full symmetry invariant across all offsets [-100..100] in 0.1 increments
        for (i in -1000..1000) {
            val offset = i / 10f
            assertEquals(
                "Symmetry broken for offset $offset",
                WhiteBalanceMath.offsetToDisplay(offset),
                -WhiteBalanceMath.offsetToDisplay(-offset)
            )
        }
    }
}
