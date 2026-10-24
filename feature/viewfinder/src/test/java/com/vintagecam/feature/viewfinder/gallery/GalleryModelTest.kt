package com.vintagecam.feature.viewfinder.gallery

import android.net.Uri
import com.vintagecam.domain.presets.model.AspectRatioProfile
import com.vintagecam.feature.viewfinder.model.FramingGeometryHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for gallery domain models, categorization, square crop mathematics,
 * spatial hit testing, and multi-selection state machines.
 */
class GalleryModelTest {

    private fun createDummyItem(
        id: Long,
        name: String,
        mediaType: MediaType,
        isCamera: Boolean = true
    ): GalleryItem {
        return GalleryItem(
            id = id,
            uri = Uri.EMPTY,
            displayName = name,
            dateTaken = System.currentTimeMillis() - id * 1000,
            mediaType = mediaType,
            isCameraPhoto = isCamera
        )
    }

    @Test
    fun testCategoryFiltering() {
        val items = listOf(
            createDummyItem(1, "RIZZ_photo1.jpg", MediaType.IMAGE, isCamera = true),
            createDummyItem(2, "RIZZ_photo2.jpg", MediaType.IMAGE, isCamera = true),
            createDummyItem(3, "RIZZ_video1.mp4", MediaType.VIDEO, isCamera = true),
            createDummyItem(4, "download_graphic.png", MediaType.OTHER, isCamera = false)
        )

        // ALL photos
        val allItems = items
        assertEquals(4, allItems.size)

        // IMAGES only
        val imageItems = items.filter { it.mediaType == MediaType.IMAGE }
        assertEquals(2, imageItems.size)
        assertTrue(imageItems.all { it.mediaType == MediaType.IMAGE })

        // VIDEOS only
        val videoItems = items.filter { it.mediaType == MediaType.VIDEO }
        assertEquals(1, videoItems.size)
        assertEquals(3L, videoItems[0].id)

        // OTHERS
        val otherItems = items.filter { it.mediaType == MediaType.OTHER || !it.isCameraPhoto }
        assertEquals(1, otherItems.size)
        assertEquals(4L, otherItems[0].id)
    }

    @Test
    fun testAllPhotosStoredAsSquareAspectCropMath() {
        // Standard sensor sizes: 4:3 portrait, 3:4 landscape, 16:9
        val sensorSizes = listOf(
            Pair(3000f, 4000f), // 3:4 portrait
            Pair(4032f, 3024f), // 4:3 landscape
            Pair(1080f, 1920f), // 9:16 portrait
            Pair(2000f, 2000f)  // Already square
        )

        for ((w, h) in sensorSizes) {
            val (normW, normH) = FramingGeometryHelper.calculateNormalizedFraming(
                containerWidth = w,
                containerHeight = h,
                aspectRatio = AspectRatioProfile.RATIO_1_1,
                focalLengthMm = 13 // Base full capture
            )

            val boxW = w * normW
            val boxH = h * normH

            // Width and height of 1:1 square crop must be strictly equal
            assertEquals(
                "Square width and height must match for container ${w}x${h}",
                boxW,
                boxH,
                0.01f
            )
        }
    }

    @Test
    fun testSlideSelectionAccumulation() {
        var selectedIds = emptySet<Long>()

        // Simulate sliding finger over item 10, then 11, then 12
        val slideSequence = listOf(10L, 11L, 12L)

        for (id in slideSequence) {
            selectedIds = selectedIds + id
        }

        assertEquals(3, selectedIds.size)
        assertTrue(selectedIds.contains(10L))
        assertTrue(selectedIds.contains(11L))
        assertTrue(selectedIds.contains(12L))

        // Sliding back over item 11 should not duplicate or remove it
        selectedIds = selectedIds + 11L
        assertEquals(3, selectedIds.size)

        // Deselection
        selectedIds = selectedIds - 11L
        assertEquals(2, selectedIds.size)
        assertFalse(selectedIds.contains(11L))
    }

    @Test
    fun testSelectAllToggleLogic() {
        val allIds = setOf(1L, 2L, 3L, 4L)
        var selectedIds = emptySet<Long>()

        // 1. None selected -> Select All selects all
        selectedIds = if (selectedIds.size == allIds.size) emptySet() else allIds
        assertEquals(4, selectedIds.size)
        assertEquals(allIds, selectedIds)

        // 2. All selected -> Select All clears selection
        selectedIds = if (selectedIds.size == allIds.size) emptySet() else allIds
        assertEquals(0, selectedIds.size)

        // 3. Partially selected -> Select All selects all
        selectedIds = setOf(1L, 2L)
        selectedIds = if (selectedIds.size == allIds.size) emptySet() else allIds
        assertEquals(4, selectedIds.size)
    }

    @Test
    fun testSpatialItemHitTesting() {
        data class MockItemInfo(val key: Long, val x: Float, val y: Float, val w: Float, val h: Float)

        val items = listOf(
            MockItemInfo(key = 101L, x = 0f, y = 0f, w = 360f, h = 360f),
            MockItemInfo(key = 102L, x = 362f, y = 0f, w = 360f, h = 360f),
            MockItemInfo(key = 103L, x = 724f, y = 0f, w = 360f, h = 360f),
            MockItemInfo(key = 104L, x = 0f, y = 362f, w = 360f, h = 360f)
        )

        fun findKey(px: Float, py: Float): Long? {
            return items.firstOrNull { item ->
                px in item.x..(item.x + item.w) && py in item.y..(item.y + item.h)
            }?.key
        }

        // Inside item 101
        assertEquals(101L, findKey(50f, 50f))
        // Inside item 102
        assertEquals(102L, findKey(400f, 100f))
        // Inside item 103
        assertEquals(103L, findKey(800f, 200f))
        // Inside item 104
        assertEquals(104L, findKey(100f, 500f))
        // Out of bounds
        assertNull(findKey(1200f, 500f))
    }

    @Test
    fun testDurationFormatting() {
        fun formatDuration(durationMs: Long): String {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
        }

        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:05", formatDuration(5000L))
        assertEquals("1:15", formatDuration(75000L))
        assertEquals("10:00", formatDuration(600000L))
    }
}
