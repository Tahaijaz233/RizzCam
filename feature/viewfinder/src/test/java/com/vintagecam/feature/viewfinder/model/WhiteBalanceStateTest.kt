package com.vintagecam.feature.viewfinder.model

import com.vintagecam.core.common.math.WhiteBalanceMath
import com.vintagecam.core.common.model.WhiteBalanceState
import com.vintagecam.feature.viewfinder.ui.components.TempTrackStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class WhiteBalanceStateTest {

    @Test
    fun testDefaultStateIsNeutralManual() {
        val defaultState = WhiteBalanceState()
        assertEquals(0f, defaultState.offset, 0.001f)
        assertEquals(0f, defaultState.tint, 0.001f)
        assertFalse("Default state per spec is manual (isAuto = false)", defaultState.isAuto)
        assertEquals(0, defaultState.offsetDisplay)
        assertEquals(6500, defaultState.kelvinLabel)
        assertEquals("0", WhiteBalanceMath.formatSigned(defaultState.offsetDisplay))
    }

    @Test
    fun testCoolColorTemperatureState() {
        // -90 offset maps to display offset -9 (cooler blue cast)
        val coolState = WhiteBalanceState(offset = -90f, isAuto = false)
        assertEquals(-9, coolState.offsetDisplay)
        assertFalse(coolState.isAuto)
        assertTrue("Cool Kelvin must be greater than 6500K", coolState.kelvinLabel > 6500)
        assertEquals("−9", WhiteBalanceMath.formatSigned(coolState.offsetDisplay))
    }

    @Test
    fun testWarmColorTemperatureState() {
        // +70 offset maps to display offset +7 (warmer amber cast)
        val warmState = WhiteBalanceState(offset = 70f, isAuto = false)
        assertEquals(7, warmState.offsetDisplay)
        assertFalse(warmState.isAuto)
        assertTrue("Warm Kelvin must be less than 6500K", warmState.kelvinLabel < 6500)
        assertEquals("+7", WhiteBalanceMath.formatSigned(warmState.offsetDisplay))
    }

    @Test
    fun testBoundaryValues() {
        val minCool = WhiteBalanceState(offset = -100f, isAuto = false)
        assertEquals(-10, minCool.offsetDisplay)
        assertEquals(10000, minCool.kelvinLabel)
        assertEquals("−10", WhiteBalanceMath.formatSigned(minCool.offsetDisplay))

        val maxWarm = WhiteBalanceState(offset = 100f, isAuto = false)
        assertEquals(10, maxWarm.offsetDisplay)
        assertEquals(2000, maxWarm.kelvinLabel)
        assertEquals("+10", WhiteBalanceMath.formatSigned(maxWarm.offsetDisplay))
    }

    @Test
    fun testTrackStylesExist() {
        val spectrum = TempTrackStyle.SPECTRUM
        val physical = TempTrackStyle.PHYSICAL

        assertEquals(2, TempTrackStyle.values().size)
        assertTrue(TempTrackStyle.values().contains(spectrum))
        assertTrue(TempTrackStyle.values().contains(physical))
    }

    @Test
    fun testWbParamsConversion() {
        // Auto mode -> strength should be 0.0f (shader bypass)
        val autoState = WhiteBalanceState(offset = 0f, tint = 0f, isAuto = true)
        val autoParams = autoState.toWbParams()
        assertEquals(6500f, autoParams.temperature, 1f)
        assertEquals(0f, autoParams.tint, 0.001f)
        assertEquals(0f, autoParams.strength, 0.001f)

        // Manual mode (-90 offset -> cool ~9489K, +15 tint) -> strength should be 1.0f
        val manualState = WhiteBalanceState(offset = -90f, tint = 15f, isAuto = false)
        val manualParams = manualState.toWbParams()
        assertTrue(manualParams.temperature > 6500f)
        assertEquals(15f, manualParams.tint, 0.001f)
        assertEquals(1.0f, manualParams.strength, 0.001f)
    }

    @Test
    fun testSliderDragAndLabelUpdates() {
        var state = WhiteBalanceState(offset = 0f, isAuto = false)
        assertFalse(state.isAuto)

        // Drag knob towards cool (-53 offset -> -5 display, ~7953K)
        val newOffset = -53f
        state = state.copy(offset = newOffset, isAuto = false)
        assertFalse(state.isAuto)
        assertEquals(-5, state.offsetDisplay)
        assertEquals("−5", WhiteBalanceMath.formatSigned(state.offsetDisplay))
        assertTrue(state.kelvinLabel > 6500)

        // Drag knob towards warm (+42 offset -> +4 display, ~4761K)
        state = state.copy(offset = 42f, isAuto = false)
        assertEquals(4, state.offsetDisplay)
        assertEquals("+4", WhiteBalanceMath.formatSigned(state.offsetDisplay))
        assertTrue(state.kelvinLabel < 6500)

        // Reset action -> returns to 0 neutral daylight with isAuto = false
        state = state.copy(offset = 0f, tint = 0f, isAuto = false)
        assertFalse(state.isAuto)
        assertEquals(0, state.offsetDisplay)
        assertEquals(6500, state.kelvinLabel)
        assertEquals("0", WhiteBalanceMath.formatSigned(state.offsetDisplay))
    }

    @Test
    fun testAutoTogglePreservesManualOffset() {
        // Start in manual mode at -40 offset
        var state = WhiteBalanceState(offset = -40f, isAuto = false)
        assertEquals(-4, state.offsetDisplay)
        assertEquals(1.0f, state.toWbParams().strength, 0.001f)

        // Tap 'A' -> enables auto, freezes manual offset, shader bypass (strength = 0)
        state = state.copy(isAuto = true)
        assertTrue(state.isAuto)
        assertEquals(-40f, state.offset, 0.001f)
        assertEquals(0.0f, state.toWbParams().strength, 0.001f)

        // Tap 'A' again -> disables auto, restores frozen manual offset, shader active (strength = 1)
        state = state.copy(isAuto = false)
        assertFalse(state.isAuto)
        assertEquals(-40f, state.offset, 0.001f)
        assertEquals(-4, state.offsetDisplay)
        assertEquals(1.0f, state.toWbParams().strength, 0.001f)
    }

    @Test
    fun testSunAndBulbStepAndRampSemantics() {
        var state = WhiteBalanceState(offset = 0f, isAuto = false)

        // Sun tap: +1 display unit (+10 internal)
        val sunStep = (state.offset + 10f).coerceIn(-100f, 100f)
        state = state.copy(offset = sunStep, isAuto = false)
        assertEquals(10f, state.offset, 0.001f)
        assertEquals(1, state.offsetDisplay)
        assertEquals("+1", WhiteBalanceMath.formatSigned(state.offsetDisplay))

        // Sun hold ramp: 5 ticks of +1.5 -> +7.5 internal
        for (i in 1..5) {
            val ramp = (state.offset + 1.5f).coerceIn(-100f, 100f)
            state = state.copy(offset = ramp, isAuto = false)
        }
        assertEquals(17.5f, state.offset, 0.001f)
        assertEquals(2, state.offsetDisplay)
        assertEquals("+2", WhiteBalanceMath.formatSigned(state.offsetDisplay))

        // Bulb tap: -1 display unit (-10 internal)
        val bulbStep = (state.offset - 10f).coerceIn(-100f, 100f)
        state = state.copy(offset = bulbStep, isAuto = false)
        assertEquals(7.5f, state.offset, 0.001f)
        assertEquals(1, state.offsetDisplay)

        // Bulb ramp across zero towards cool: 16 ticks of -1.5 -> -24.0 internal (from 7.5 -> -16.5)
        for (i in 1..16) {
            val ramp = (state.offset - 1.5f).coerceIn(-100f, 100f)
            state = state.copy(offset = ramp, isAuto = false)
        }
        assertEquals(-16.5f, state.offset, 0.001f)
        assertEquals(-2, state.offsetDisplay)
        assertEquals("−2", WhiteBalanceMath.formatSigned(state.offsetDisplay))
    }

    @Test
    fun testResetActionClearsAutoMode() {
        // Start in auto mode with custom manual offset frozen
        val autoState = WhiteBalanceState(offset = 40f, isAuto = true)
        assertTrue(autoState.isAuto)

        // Reset must clear offset to 0 and explicitly deactivate auto mode
        val resetState = autoState.copy(offset = 0f, tint = 0f, isAuto = false)
        assertFalse(resetState.isAuto)
        assertEquals(0f, resetState.offset, 0.001f)
        assertEquals(0, resetState.offsetDisplay)
        assertEquals(6500, resetState.kelvinLabel)
        assertEquals(1.0f, resetState.toWbParams().strength, 0.001f)
    }

    @Test
    fun testDisplayOffsetClampedDuringOvershoot() {
        // Extreme touch drag rubber-band overshoots past [-100..100]
        val overCool = WhiteBalanceState(offset = -115f, isAuto = false)
        assertEquals(-10, overCool.offsetDisplay)
        assertEquals("−10", WhiteBalanceMath.formatSigned(overCool.offsetDisplay))

        val overWarm = WhiteBalanceState(offset = 118f, isAuto = false)
        assertEquals(10, overWarm.offsetDisplay)
        assertEquals("+10", WhiteBalanceMath.formatSigned(overWarm.offsetDisplay))
    }

    @Test
    fun testZeroCrossingDetentConditions() {
        fun isZeroCrossing(prev: Int, next: Int): Boolean {
            return prev == 0 || next == 0 || (prev < 0 && next > 0) || (prev > 0 && next < 0)
        }

        // Stepping away from 0 into warm
        assertTrue(isZeroCrossing(0, 1))
        // Stepping away from 0 into cool
        assertTrue(isZeroCrossing(0, -1))
        // Stepping back to 0 from warm
        assertTrue(isZeroCrossing(1, 0))
        // Stepping back to 0 from cool
        assertTrue(isZeroCrossing(-1, 0))
        // Stepping across 0 directly
        assertTrue(isZeroCrossing(-1, 1))
        assertTrue(isZeroCrossing(1, -1))

        // Non-zero transitions (should be regular tick, NOT double tick)
        assertFalse(isZeroCrossing(1, 2))
        assertFalse(isZeroCrossing(2, 3))
        assertFalse(isZeroCrossing(-1, -2))
        assertFalse(isZeroCrossing(-2, -3))
    }
}
