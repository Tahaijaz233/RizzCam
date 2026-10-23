package com.vintagecam.core.camera.diptych

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

/**
 * State machine for the Olympus PEN half-frame camera emulation (`FS 400H Half`).
 */
sealed interface DiptychState {
    /**
     * Initial or cleared state. Awaiting Shot 1 (left frame).
     */
    data object Empty : DiptychState

    /**
     * Shot 1 has been captured and stored. Awaiting Shot 2 (right frame) to trigger stitching.
     */
    data class FirstFrameReady(
        val firstFrame: Bitmap,
        val timestamp: Long = System.currentTimeMillis()
    ) : DiptychState
}

/**
 * Result returned by [HalfFrameDiptychManager.processFrame].
 */
sealed interface DiptychResult {
    /**
     * Shot 1 registered. The diptych is waiting for Shot 2.
     */
    data class FirstFrameStored(val firstFrameThumbnail: Bitmap) : DiptychResult

    /**
     * Shot 2 registered. Both frames were stitched side-by-side into a 3:2 landscape composite.
     * The input frame bitmaps have been recycled deterministically.
     */
    data class CompositeReady(val compositeBitmap: Bitmap) : DiptychResult
}

/**
 * Manages the 2-shot half-frame exposure cycle of classic 35mm half-frame cameras (e.g. Olympus PEN).
 *
 * In half-frame photography, the standard 36x24mm 3:2 landscape film frame is split into two
 * adjacent 18x24mm 3:4 portrait frames.
 *
 * Capabilities:
 * - State machine transitioning from [DiptychState.Empty] -> [DiptychState.FirstFrameReady] -> Composite.
 * - Stitches Shot 1 (left) and Shot 2 (right) into a unified 3:2 landscape composite.
 * - Injects a physical 14px dark vertical divider strip with subtle film gate edge lines.
 * - Strictly recycles intermediate individual bitmaps once the composite is assembled.
 * - Provides [cancelPendingFrame] to discard and recycle pending frames when cancelling or switching presets.
 */
class HalfFrameDiptychManager {

    companion object {
        private const val TAG = "HalfFrameDiptychManager"
        const val DIVIDER_WIDTH_PX = 14
        private const val GATE_BG_COLOR = 0xFF0D0D0D.toInt()
        private const val GATE_EDGE_COLOR = 0xFF242424.toInt()
        private const val THUMBNAIL_SIZE_PX = 160
    }

    private val _state = MutableStateFlow<DiptychState>(DiptychState.Empty)
    val state: StateFlow<DiptychState> = _state.asStateFlow()

    private val lock = Any()

    /**
     * Ingests a captured 3:4 still frame into the diptych state machine.
     *
     * @param frameBitmap Upright, color-graded [Bitmap].
     * @return [DiptychResult.FirstFrameStored] if this is Shot 1, or [DiptychResult.CompositeReady] if this is Shot 2.
     */
    fun processFrame(frameBitmap: Bitmap): DiptychResult = synchronized(lock) {
        val currentState = _state.value

        when (currentState) {
            is DiptychState.Empty -> {
                Log.i(TAG, "Shot 1 received. Storing first frame for half-frame diptych.")
                _state.value = DiptychState.FirstFrameReady(frameBitmap)

                // Generate lightweight thumbnail for viewfinder diptych progress preview
                val thumb = generateThumbnail(frameBitmap)
                DiptychResult.FirstFrameStored(thumb)
            }

            is DiptychState.FirstFrameReady -> {
                Log.i(TAG, "Shot 2 received. Stitching 3:4 frames into 3:2 landscape diptych composite.")
                val frame1 = currentState.firstFrame
                val frame2 = frameBitmap

                try {
                    val composite = stitchDiptych(frame1, frame2)
                    _state.value = DiptychState.Empty
                    DiptychResult.CompositeReady(composite)
                } finally {
                    // Strictly recycle both source bitmaps immediately after canvas drawing
                    if (!frame1.isRecycled) {
                        frame1.recycle()
                    }
                    if (!frame2.isRecycled) {
                        frame2.recycle()
                    }
                }
            }
        }
    }

    /**
     * Stitches Frame 1 (left 3:4) and Frame 2 (right 3:4) side-by-side with a 14px dark divider.
     */
    private fun stitchDiptych(frame1: Bitmap, frame2: Bitmap): Bitmap {
        // Normalize height to maximum resolution of the two captures
        val targetHeight = maxOf(frame1.height, frame2.height)

        // Calculate 3:4 frame width corresponding to targetHeight
        val frameWidth = (targetHeight * 3.0f / 4.0f).roundToInt()

        // Total width: Frame1 (3:4) + 14px Divider + Frame2 (3:4) ≈ 3:2 Landscape
        val totalWidth = (frameWidth * 2) + DIVIDER_WIDTH_PX

        val composite = Bitmap.createBitmap(totalWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composite)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val rectPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // 1. Draw outer film gate canvas background
        rectPaint.color = GATE_BG_COLOR
        canvas.drawRect(0f, 0f, totalWidth.toFloat(), targetHeight.toFloat(), rectPaint)

        // 2. Draw Left Frame (Shot 1) with optical 3:4 aspect center crop
        val srcRect1 = calculateCenterCropSrcRect(frame1.width, frame1.height, 3f / 4f)
        val dstRect1 = Rect(0, 0, frameWidth, targetHeight)
        canvas.drawBitmap(frame1, srcRect1, dstRect1, paint)

        // 3. Draw Right Frame (Shot 2) with optical 3:4 aspect center crop
        val frame2Left = frameWidth + DIVIDER_WIDTH_PX
        val srcRect2 = calculateCenterCropSrcRect(frame2.width, frame2.height, 3f / 4f)
        val dstRect2 = Rect(frame2Left, 0, totalWidth, targetHeight)
        canvas.drawBitmap(frame2, srcRect2, dstRect2, paint)

        // 4. Draw 14px Dark Film Gate Divider Strip
        rectPaint.color = GATE_BG_COLOR
        canvas.drawRect(
            frameWidth.toFloat(),
            0f,
            (frameWidth + DIVIDER_WIDTH_PX).toFloat(),
            targetHeight.toFloat(),
            rectPaint
        )

        // 5. Subtle Film Gate Edge Lines (simulating physical metal frame mask separation)
        val edgePaint = Paint().apply {
            color = GATE_EDGE_COLOR
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        // Left boundary edge line
        canvas.drawLine(
            frameWidth.toFloat(),
            0f,
            frameWidth.toFloat(),
            targetHeight.toFloat(),
            edgePaint
        )
        // Right boundary edge line
        canvas.drawLine(
            (frameWidth + DIVIDER_WIDTH_PX - 1).toFloat(),
            0f,
            (frameWidth + DIVIDER_WIDTH_PX - 1).toFloat(),
            targetHeight.toFloat(),
            edgePaint
        )

        Log.i(TAG, "Assembled diptych composite: ${composite.width}x${composite.height}")
        return composite
    }

    /**
     * Calculates a centered source crop rectangle adhering strictly to [targetAspect]
     * to eliminate aspect ratio distortion during diptych composition.
     */
    private fun calculateCenterCropSrcRect(bitmapWidth: Int, bitmapHeight: Int, targetAspect: Float): Rect {
        val srcAspect = bitmapWidth.toFloat() / bitmapHeight.toFloat()
        return if (srcAspect > targetAspect) {
            val cropWidth = (bitmapHeight * targetAspect).roundToInt().coerceAtMost(bitmapWidth)
            val left = (bitmapWidth - cropWidth) / 2
            Rect(left, 0, left + cropWidth, bitmapHeight)
        } else {
            val cropHeight = (bitmapWidth / targetAspect).roundToInt().coerceAtMost(bitmapHeight)
            val top = (bitmapHeight - cropHeight) / 2
            Rect(0, top, bitmapWidth, top + cropHeight)
        }
    }

    private fun generateThumbnail(source: Bitmap): Bitmap {
        val aspect = source.width.toFloat() / source.height.toFloat().coerceAtLeast(1f)
        val thumbWidth = (THUMBNAIL_SIZE_PX * aspect).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, thumbWidth, THUMBNAIL_SIZE_PX, true)
    }

    /**
     * Cancels any pending first frame and recycles its allocated bitmap memory immediately.
     */
    fun cancelPendingFrame() = synchronized(lock) {
        val currentState = _state.value
        if (currentState is DiptychState.FirstFrameReady) {
            Log.i(TAG, "Cancelling pending first frame of diptych and recycling memory")
            if (!currentState.firstFrame.isRecycled) {
                currentState.firstFrame.recycle()
            }
            _state.value = DiptychState.Empty
        }
    }

    /**
     * Resets the diptych manager to [DiptychState.Empty].
     */
    fun reset() {
        cancelPendingFrame()
    }
}
