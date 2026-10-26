package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.vintagecam.core.common.math.WhiteBalanceMath
import com.vintagecam.feature.viewfinder.R
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.theme.ChromeTokens
import com.vintagecam.feature.viewfinder.theme.Motion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Capsule gradient slider with 1:1 finger tracking, rubber-band resistance,
 * tactile detent haptics, and double-tap reset (Section 7.5 & Appendix B).
 */
@Composable
fun TemperatureSlider(
    offset: Float,
    onOffsetChange: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    trackStyle: TempTrackStyle = TempTrackStyle.SPECTRUM,
    haptics: RetroHapticsEngine? = null
) {
    val context = LocalContext.current
    val effectiveHaptics = haptics ?: remember(context) { RetroHapticsEngine(context) }
    val coroutineScope = rememberCoroutineScope()
    var widthPx by remember { mutableFloatStateOf(1f) }
    val visualAnimatable = remember { Animatable(offset) }
    var isDragging by remember { androidx.compose.runtime.mutableStateOf(false) }
    var dragVisualOffset by remember { mutableFloatStateOf(offset) }
    val lastTick = remember { mutableIntStateOf((offset / 10f).roundToInt()) }
    val currentOffset by rememberUpdatedState(offset)
    val currentOnOffsetChange by rememberUpdatedState(onOffsetChange)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnReset by rememberUpdatedState(onReset)

    LaunchedEffect(offset, isDragging) {
        if (!isDragging && !visualAnimatable.isRunning && visualAnimatable.value != offset) {
            visualAnimatable.snapTo(offset)
        }
    }

    val displayVal = (offset / 10f).roundToInt()
    val sliderDesc = stringResource(R.string.temp_slider_desc)
    val a11yText = "Color temperature, ${WhiteBalanceMath.formatSigned(displayVal)}. Double-tap to reset."

    Box(
        modifier = modifier
            .height(44.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .semantics(mergeDescendants = true) {
                this.contentDescription = a11yText
                this.progressBarRangeInfo = ProgressBarRangeInfo(offset, -100f..100f)
                this.setProgress(sliderDesc) { target ->
                    val clamped = target.coerceIn(-100f, 100f)
                    currentOnOffsetChange(clamped)
                    currentOnDragEnd()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        effectiveHaptics.heavyClick()
                        coroutineScope.launch {
                            visualAnimatable.animateTo(0f, Motion.PillSpring)
                        }
                        currentOnReset()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragVisualOffset = currentOffset
                        lastTick.intValue = (currentOffset / 10f).roundToInt()
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            visualAnimatable.snapTo(dragVisualOffset)
                            visualAnimatable.animateTo(
                                targetValue = currentOffset.coerceIn(-100f, 100f),
                                animationSpec = Motion.PillSpring
                            )
                            isDragging = false
                        }
                        currentOnDragEnd()
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            visualAnimatable.snapTo(dragVisualOffset)
                            visualAnimatable.animateTo(
                                targetValue = currentOffset.coerceIn(-100f, 100f),
                                animationSpec = Motion.PillSpring
                            )
                            isDragging = false
                        }
                        currentOnDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val deltaOffset = (dragAmount.x / (widthPx / 2f)) * 100f
                        val rawNext = dragVisualOffset + deltaOffset
                        val rubberBanded = WhiteBalanceMath.rubberBand(rawNext, -100f, 100f, 0.35f)
                        dragVisualOffset = rubberBanded

                        val applied = rubberBanded.coerceIn(-100f, 100f)
                        val detent = (applied / 10f).roundToInt()
                        if (detent != lastTick.intValue) {
                            lastTick.intValue = detent
                            if (detent == 0) {
                                effectiveHaptics.doubleClick()
                            } else {
                                effectiveHaptics.tick()
                            }
                        }
                        currentOnOffsetChange(applied)
                    }
                )
            }
    ) {
        val currentKnobOffset = if (isDragging) dragVisualOffset else visualAnimatable.value

        Canvas(modifier = Modifier.matchParentSize()) {
            val trackH = ChromeTokens.TrackH.toPx()
            val trackTop = (size.height - trackH) / 2f
            val cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)

            // 1. Draw capsule gradient track
            drawRoundRect(
                brush = trackStyle.brush(),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackH),
                cornerRadius = cornerRadius
            )

            // 2. Horizontal dotted crosshair line along the center of the track
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f)
            drawLine(
                color = Color.White.copy(alpha = 0.40f),
                start = Offset(trackH / 2f, size.height / 2f),
                end = Offset(size.width - trackH / 2f, size.height / 2f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )

            // 3. Center detent hairline marker (at 0 offset)
            val centerHairlineHeight = trackH - 4.dp.toPx()
            drawRect(
                color = Color.White.copy(alpha = 0.70f),
                topLeft = Offset(size.width / 2f - 0.5.dp.toPx(), trackTop + 2.dp.toPx()),
                size = Size(1.dp.toPx(), centerHairlineHeight)
            )

            // 4. Subtle track border for specular finish
            drawRoundRect(
                color = Color.White.copy(alpha = 0.20f),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackH),
                cornerRadius = cornerRadius,
                style = Stroke(width = 0.8.dp.toPx())
            )

            // 5. Black dot knob with 1.5 dp white ring stroke (tracks visualOffset with rubber-band overshoot)
            val knobRadius = ChromeTokens.KnobD.toPx() / 2f
            val usableTravel = (size.width - 2f * knobRadius).coerceAtLeast(1f)
            val fraction = (currentKnobOffset + 100f) / 200f
            val knobX = knobRadius + fraction * usableTravel

            drawCircle(
                color = Color.Black,
                radius = knobRadius,
                center = Offset(knobX, size.height / 2f)
            )
            drawCircle(
                color = Color.White,
                radius = knobRadius,
                center = Offset(knobX, size.height / 2f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
