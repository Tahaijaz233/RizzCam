package com.vintagecam.feature.viewfinder.model

import com.vintagecam.domain.presets.model.AspectRatioProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating optical framing geometry calculations across all focal lengths
 * and aspect ratios.
 */
class FramingGeometryHelperTest {

    @Test
    fun testEveryMillimeterFrom13To135ProducesStrictlyMonotonicFramingScale() {
        val containerW = 1080f
        val containerH = 1440f // 3:4 container
        val aspectRatio = AspectRatioProfile.RATIO_3_2

        var previousWidth = Float.MAX_VALUE
        var previousHeight = Float.MAX_VALUE

        for (mm in 13..135) {
            val (normW, normH) = FramingGeometryHelper.calculateNormalizedFraming(
                containerWidth = containerW,
                containerHeight = containerH,
                aspectRatio = aspectRatio,
                focalLengthMm = mm
            )

            val boxW = containerW * normW
            val boxH = containerH * normH

            // Each increasing millimeter must strictly shrink the framing box
            assertTrue(
                "At ${mm}mm, box width ($boxW) should be strictly less than previous ($previousWidth)",
                boxW < previousWidth
            )
            assertTrue(
                "At ${mm}mm, box height ($boxH) should be strictly less than previous ($previousHeight)",
                boxH < previousHeight
            )

            // Aspect ratio of box must strictly match 2:3 portrait (2/3)
            val expectedRatio = 2f / 3f
            val actualRatio = boxW / boxH
            assertEquals("Aspect ratio at ${mm}mm", expectedRatio, actualRatio, 0.01f)

            previousWidth = boxW
            previousHeight = boxH
        }
    }

    @Test
    fun testClampingBelowMinAndAboveMaxFocalLength() {
        val containerW = 1080f
        val containerH = 1440f
        val aspect = AspectRatioProfile.RATIO_4_3

        val geom13 = FramingGeometryHelper.calculateNormalizedFraming(containerW, containerH, aspect, 13)
        val geomBelow13 = FramingGeometryHelper.calculateNormalizedFraming(containerW, containerH, aspect, 10)
        assertEquals("Clamping below 13mm matches 13mm full capture width", geom13.first, geomBelow13.first, 1e-5f)
        assertEquals("Clamping below 13mm matches 13mm full capture height", geom13.second, geomBelow13.second, 1e-5f)
        assertEquals("13mm expands to 100% full capture width", 1.0f, geom13.first, 1e-4f)
        assertEquals("13mm expands to 100% full capture height", 1.0f, geom13.second, 1e-4f)

        val geom135 = FramingGeometryHelper.calculateNormalizedFraming(containerW, containerH, aspect, 135)
        val geomAbove135 = FramingGeometryHelper.calculateNormalizedFraming(containerW, containerH, aspect, 200)
        assertEquals(geom135.first, geomAbove135.first, 1e-5f)
        assertEquals(geom135.second, geomAbove135.second, 1e-5f)
    }

    @Test
    fun testViewportCenteringSymmetry() {
        val containerW = 1080f
        val containerH = 1440f

        for (aspect in listOf(
            AspectRatioProfile.RATIO_3_2,
            AspectRatioProfile.RATIO_4_3,
            AspectRatioProfile.RATIO_1_1,
            AspectRatioProfile.RATIO_6_7
        )) {
            val viewport = FramingGeometryHelper.calculateViewportGeometry(
                containerWidth = containerW,
                containerHeight = containerH,
                aspectRatio = aspect,
                focalLengthMm = 35
            )

            assertEquals("Centered X", containerW / 2f, viewport.centerX, 1e-4f)
            assertEquals("Centered Y", containerH / 2f, viewport.centerY, 1e-4f)
            assertTrue("Left in bounds", viewport.left >= 0f)
            assertTrue("Right in bounds", viewport.right <= containerW)
            assertTrue("Top in bounds", viewport.top >= 0f)
            assertTrue("Bottom in bounds", viewport.bottom <= containerH)
        }
    }
}
