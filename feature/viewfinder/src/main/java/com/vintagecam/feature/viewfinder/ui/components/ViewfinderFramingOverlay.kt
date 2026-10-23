package com.vintagecam.feature.viewfinder.ui.components

import android.graphics.PointF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vintagecam.core.camera.model.FocusMeteringState
import com.vintagecam.domain.presets.model.AspectRatioProfile
import com.vintagecam.feature.viewfinder.model.FramingGeometryHelper
import com.vintagecam.feature.viewfinder.model.FramingRectGeometry
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Skeuomorphic Viewfinder Framing Overlay.
 *
 * Implements:
 * 1. Dark semi-transparent scrim outside the framing box (Color.Black.copy(alpha = 0.62f)).
 * 2. Clear optical view inside the framing box.
 * 3. 3x3 rule of thirds grid with subtle lines.
 * 4. High-contrast white rounded framing border (corner radius ~18dp, stroke ~1.8dp).
 * 5. Top-center focal length badge text (e.g. "35mm", "50mm").
 * 6. Smooth continuous two-finger pinch gesture on viewfinder adjusting focal length (18 to 135mm).
 * 7. Single tap gesture triggers tap-to-focus and auto-exposure metering.
 */
@Composable
fun ViewfinderFramingOverlay(
    aspectRatio: AspectRatioProfile,
    focalLengthMm: Int,
    gridEnabled: Boolean,
    focusState: FocusMeteringState,
    focusPoint: PointF?,
    onFocalLengthChanged: (Int) -> Unit,
    onFocusTap: (x: Float, y: Float, viewWidth: Float, viewHeight: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val currentFocalMm by rememberUpdatedState(focalLengthMm)

    // Continuous float tracker for buttery smooth two-finger pinch-to-zoom
    var continuousFocalMm by remember { mutableFloatStateOf(focalLengthMm.toFloat()) }
    LaunchedEffect(focalLengthMm) {
        continuousFocalMm = focalLengthMm.toFloat()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onFocusTap(offset.x, offset.y, size.width.toFloat(), size.height.toFloat())
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        continuousFocalMm = (continuousFocalMm * zoom).coerceIn(18f, 135f)
                        val newMm = continuousFocalMm.roundToInt()
                        if (newMm != currentFocalMm) {
                            onFocalLengthChanged(newMm)
                        }
                    }
                }
            }
    ) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()

        if (viewWidth > 0 && viewHeight > 0) {
            val geometry = remember(viewWidth, viewHeight, aspectRatio, focalLengthMm) {
                FramingGeometryHelper.calculateViewportGeometry(
                    containerWidth = viewWidth,
                    containerHeight = viewHeight,
                    aspectRatio = aspectRatio,
                    focalLengthMm = focalLengthMm
                )
            }

            // 1. Draw Scrim, Grid, and White Framing Border
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cornerRadiusPx = 18.dp.toPx()
                val strokeWidthPx = 1.8.dp.toPx()

                // A. Semi-transparent dark scrim outside the framing rectangle
                val scrimPath = Path().apply {
                    fillType = PathFillType.EvenOdd
                    // Full container rect
                    addRect(Rect(0f, 0f, size.width, size.height))
                    // Cut out inner rounded framing box
                    addRoundRect(
                        RoundRect(
                            left = geometry.left,
                            top = geometry.top,
                            right = geometry.right,
                            bottom = geometry.bottom,
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    )
                }
                drawPath(path = scrimPath, color = Color.Black.copy(alpha = 0.62f))

                // B. 3x3 Rule of Thirds Grid (inside the framing box)
                if (gridEnabled) {
                    val gridColor = Color.White.copy(alpha = 0.32f)
                    val gridStroke = 1.dp.toPx()

                    // Vertical thirds
                    val colW = geometry.width / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(geometry.left + colW, geometry.top),
                        end = Offset(geometry.left + colW, geometry.bottom),
                        strokeWidth = gridStroke
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(geometry.left + colW * 2f, geometry.top),
                        end = Offset(geometry.left + colW * 2f, geometry.bottom),
                        strokeWidth = gridStroke
                    )

                    // Horizontal thirds
                    val rowH = geometry.height / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(geometry.left, geometry.top + rowH),
                        end = Offset(geometry.right, geometry.top + rowH),
                        strokeWidth = gridStroke
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(geometry.left, geometry.top + rowH * 2f),
                        end = Offset(geometry.right, geometry.top + rowH * 2f),
                        strokeWidth = gridStroke
                    )
                }

                // C. Half-frame Diptych vertical frame line
                if (aspectRatio == AspectRatioProfile.HALF_FRAME_DIPTYCH) {
                    val divX = geometry.centerX
                    drawLine(
                        color = Color(0x66000000),
                        start = Offset(divX - 1f, geometry.top),
                        end = Offset(divX - 1f, geometry.bottom),
                        strokeWidth = 2.5f
                    )
                    drawLine(
                        color = Color(0x44FFFFFF),
                        start = Offset(divX + 1f, geometry.top),
                        end = Offset(divX + 1f, geometry.bottom),
                        strokeWidth = 1.5f
                    )
                }

                // D. Rounded Rect White Framing Border
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(geometry.left, geometry.top),
                    size = Size(geometry.width, geometry.height),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = Stroke(width = strokeWidthPx)
                )
            }

            // 2. Focal Length Text Label centered at top of the framing border ("35mm")
            val labelOffsetY = with(density) { (geometry.top - 24.dp.toPx()).roundToInt() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Text(
                    text = "${focalLengthMm}mm",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset { IntOffset(0, labelOffsetY) }
                )
            }

            // 3. Golden tap-to-focus animated reticle
            if (focusPoint != null && focusState != FocusMeteringState.IDLE) {
                FocusReticleOverlay(
                    focusPoint = focusPoint,
                    focusState = focusState
                )
            }
        }
    }
}

/**
 * Animated tap-to-focus reticle with tactile pulsating corner brackets.
 */
@Composable
private fun FocusReticleOverlay(
    focusPoint: PointF,
    focusState: FocusMeteringState,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(1.35f) }
    val alphaAnim = remember { Animatable(1.0f) }

    val reticleColor = when (focusState) {
        FocusMeteringState.FOCUSING -> Color(0xFFFACC15)
        FocusMeteringState.SUCCESS -> Color(0xFF4ADE80)
        FocusMeteringState.FAILED -> Color(0xFFEF4444)
        FocusMeteringState.IDLE -> Color.Transparent
    }

    LaunchedEffect(focusPoint) {
        scaleAnim.snapTo(1.35f)
        alphaAnim.snapTo(1.0f)

        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
        delay(1600)
        alphaAnim.animateTo(
            targetValue = 0.0f,
            animationSpec = tween(durationMillis = 350)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = focusPoint.x
        val cy = focusPoint.y
        val radius = 26.dp.toPx() * scaleAnim.value
        val corner = 9.dp.toPx()
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val color = reticleColor.copy(alpha = alphaAnim.value)

        // Center dot
        drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(cx, cy))

        // 4 corner brackets
        val path = Path().apply {
            // Top-Left
            moveTo(cx - radius, cy - radius + corner)
            lineTo(cx - radius, cy - radius)
            lineTo(cx - radius + corner, cy - radius)

            // Top-Right
            moveTo(cx + radius - corner, cy - radius)
            lineTo(cx + radius, cy - radius)
            lineTo(cx + radius, cy - radius + corner)

            // Bottom-Right
            moveTo(cx + radius, cy + radius - corner)
            lineTo(cx + radius, cy + radius)
            lineTo(cx + radius - corner, cy + radius)

            // Bottom-Left
            moveTo(cx - radius + corner, cy + radius)
            lineTo(cx - radius, cy + radius)
            lineTo(cx - radius, cy + radius - corner)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}
