package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.theme.RetroTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Skeuomorphic knurled metal thumbwheel for manual exposure compensation (EV).
 *
 * Modeled after analog camera compensation dials (Leica M, Contax T2, Fuji X100):
 * - Tactile rotary metal cylinder with vertical knurling micro-grooves.
 * - 1/3 EV click stops covering -2.0 EV to +2.0 EV (13 physical detents).
 * - Center knurled brass index needle.
 * - Fires [RetroHapticsEngine.tick] on every physical step boundary.
 * - Magnetic spring snap to discrete 1/3 stops upon thumb release.
 */
@Composable
fun KnurledExposureDial(
    currentEv: Float,
    onEvChanged: (Float) -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier,
    width: Dp = 190.dp,
    height: Dp = 50.dp
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = RetroTheme.colors
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Pre-allocated text paints to guarantee zero heap allocations during 60 FPS dial dragging
    val textPaint = remember(colors.vintageWhite, density) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            color = colors.vintageWhite.toArgb()
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.SANS_SERIF,
                android.graphics.Typeface.BOLD
            )
        }
    }

    val activeTextPaint = remember(colors.knurledBrass, density) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = with(density) { 11.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            color = colors.knurledBrass.toArgb()
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.SANS_SERIF,
                android.graphics.Typeface.BOLD
            )
        }
    }

    // Range: -2.0 EV to +2.0 EV in 1/3 stop increments -> indices -6 to +6
    val currentStepIndex = remember(currentEv) {
        (currentEv / (1f / 3f)).roundToInt().coerceIn(-6, 6)
    }

    // Pixels per 1/3 stop on the dial
    val stepSpacingPx = 36f

    // Animated offset tracking horizontal drag
    val dragOffset = remember { Animatable(-currentStepIndex * stepSpacingPx) }
    var lastHapticIndex by remember { mutableIntStateOf(currentStepIndex) }

    val currentOnEvChanged by rememberUpdatedState(onEvChanged)

    // Sync external EV changes with internal dial position
    LaunchedEffect(currentStepIndex) {
        val targetOffset = -currentStepIndex * stepSpacingPx
        if ((dragOffset.value - targetOffset).let { it > 1f || it < -1f }) {
            dragOffset.animateTo(
                targetValue = targetOffset,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newOffset = (dragOffset.value + dragAmount.x)
                                .coerceIn(-6 * stepSpacingPx, 6 * stepSpacingPx)
                            dragOffset.snapTo(newOffset)

                            // Calculate active 1/3 step index
                            val stepIdx = (-newOffset / stepSpacingPx).roundToInt().coerceIn(-6, 6)
                            if (stepIdx != lastHapticIndex) {
                                lastHapticIndex = stepIdx
                                haptics.tick()
                                currentOnEvChanged(stepIdx * (1f / 3f))
                            }
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            val targetIndex = (-dragOffset.value / stepSpacingPx).roundToInt().coerceIn(-6, 6)
                            dragOffset.animateTo(
                                targetValue = -targetIndex * stepSpacingPx,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                            currentOnEvChanged(targetIndex * (1f / 3f))
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            val targetIndex = (-dragOffset.value / stepSpacingPx).roundToInt().coerceIn(-6, 6)
                            dragOffset.animateTo(-targetIndex * stepSpacingPx)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f

            // 1. Recessed Bezel Housing
            drawRoundRect(
                color = colors.titaniumDark,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )

            // Inner dark well shadow
            drawRoundRect(
                color = Color(0x66000000),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )

            // 2. Knurled Cylinder Surface: 3D curved lighting gradient
            val cylinderGradient = Brush.verticalGradient(
                colors = listOf(
                    colors.chassisShadow,
                    colors.titaniumMid,
                    colors.titaniumHighlight,
                    colors.titaniumMid,
                    colors.chassisShadow
                )
            )
            drawRoundRect(
                brush = cylinderGradient,
                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                size = Size(w - 8.dp.toPx(), h - 8.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
            )

            // 3. Knurling micro-ridges (drawn across visible width)
            val knurlWidth = 3f
            val knurlSpacing = 6f
            var kx = 8.dp.toPx()
            while (kx < w - 8.dp.toPx()) {
                drawLine(
                    color = Color(0x22000000),
                    start = Offset(kx, 6.dp.toPx()),
                    end = Offset(kx, h - 6.dp.toPx()),
                    strokeWidth = knurlWidth
                )
                drawLine(
                    color = Color(0x18FFFFFF),
                    start = Offset(kx + 1.5f, 6.dp.toPx()),
                    end = Offset(kx + 1.5f, h - 6.dp.toPx()),
                    strokeWidth = 1f
                )
                kx += knurlSpacing
            }

            // 4. EV Scale Markings (-6 to +6 in 1/3 stops)
            for (i in -6..6) {
                val tickX = centerX + dragOffset.value + (i * stepSpacingPx)
                // Cull off-screen ticks
                if (tickX < -20f || tickX > w + 20f) continue

                val isFullStop = (i % 3 == 0)
                val isSelected = (i == lastHapticIndex)
                val tickHeight = if (isFullStop) 14.dp.toPx() else 8.dp.toPx()
                val tickYStart = 8.dp.toPx()

                // Tick line
                drawLine(
                    color = if (isSelected) colors.knurledBrass else colors.vintageWhite.copy(alpha = if (isFullStop) 0.85f else 0.45f),
                    start = Offset(tickX, tickYStart),
                    end = Offset(tickX, tickYStart + tickHeight),
                    strokeWidth = if (isFullStop) 2f else 1.2f
                )

                // Numeric label for full stops (-2, -1, 0, +1, +2)
                if (isFullStop) {
                    val stopVal = i / 3
                    val label = when {
                        stopVal > 0 -> "+$stopVal"
                        stopVal == 0 -> "0"
                        else -> "$stopVal"
                    }

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            label,
                            tickX,
                            h - 10.dp.toPx(),
                            if (isSelected) activeTextPaint else textPaint
                        )
                    }
                }
            }

            // 5. Fixed Center Knurled Brass Index Needle
            drawLine(
                color = colors.knurledBrass,
                start = Offset(centerX, 2.dp.toPx()),
                end = Offset(centerX, 18.dp.toPx()),
                strokeWidth = 3.dp.toPx()
            )
            // Index needle brass highlight
            drawLine(
                color = colors.brassHighlight,
                start = Offset(centerX, 2.dp.toPx()),
                end = Offset(centerX, 18.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}
