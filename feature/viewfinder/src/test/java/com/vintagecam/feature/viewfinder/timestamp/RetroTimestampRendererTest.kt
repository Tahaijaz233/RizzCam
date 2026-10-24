package com.vintagecam.feature.viewfinder.timestamp

import com.vintagecam.core.camera.timestamp.RetroTimestampRenderer
import com.vintagecam.core.camera.timestamp.TimestampFormat
import com.vintagecam.core.camera.timestamp.TimestampStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

/**
 * Unit tests for [RetroTimestampRenderer] formatting and style resolution.
 */
class RetroTimestampRendererTest {

    @Test
    fun testFormatTimestampYearMonthDay() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2024)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 24)
        }
        val formatted = RetroTimestampRenderer.formatTimestamp(cal.time, TimestampFormat.YEAR_MONTH_DAY)
        assertEquals("Should match 'YY MM DD format", "'24 10 24", formatted)
    }

    @Test
    fun testFormatTimestampMonthDayYear() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2024)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 24)
        }
        val formatted = RetroTimestampRenderer.formatTimestamp(cal.time, TimestampFormat.MONTH_DAY_YEAR)
        assertEquals("Should match MM DD 'YY format", "10 24 '24", formatted)
    }

    @Test
    fun testFormatTimestampWithTime() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2024)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 24)
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 45)
        }
        val formatted = RetroTimestampRenderer.formatTimestamp(cal.time, TimestampFormat.DATE_WITH_TIME)
        assertEquals("Should match 'YY MM DD  HH:mm format", "'24 10 24  21:45", formatted)
    }

    @Test
    fun testFormatTimestampCamcorderOsd() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 1998)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 24)
        }
        val formatted = RetroTimestampRenderer.formatTimestamp(cal.time, TimestampFormat.CAMCORDER_OSD)
        assertTrue("Camcorder format must contain OCT. 24 1998", formatted.startsWith("OCT. 24 1998"))
    }

    @Test
    fun testResolveStyleForVideoAndPhoto() {
        assertEquals(
            "Auto style on video preset resolves to Camcorder Green",
            TimestampStyle.CAMCORDER_GREEN,
            RetroTimestampRenderer.resolveStyle(TimestampStyle.AUTO, isVideoPreset = true)
        )
        assertEquals(
            "Auto style on photo preset resolves to Digicam Orange",
            TimestampStyle.DIGICAM_ORANGE,
            RetroTimestampRenderer.resolveStyle(TimestampStyle.AUTO, isVideoPreset = false)
        )
        assertEquals(
            "Explicit style overrides video preset",
            TimestampStyle.RETRO_GOLD,
            RetroTimestampRenderer.resolveStyle(TimestampStyle.RETRO_GOLD, isVideoPreset = true)
        )
    }
}
