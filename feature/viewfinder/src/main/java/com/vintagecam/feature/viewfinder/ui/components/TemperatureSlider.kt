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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
 * tactile detent haptics, and double-tap reset (Section 1 & 3).
 *
 * T3: Capsule, 14 dp tall. Gradient = stylized spectrum matching reference:
 *     [#D643C8, #7A5CFF, #3E8BFF, #2FD4C4, #59E06B, #E8E34A, #FF9A3D] (cool side left).
 *     Knob: 18 dp black dot with 1.5 dp white ring, 1:1 with finger.
 *     Center detent: 1 dp white 50% notch.
 *     When isAuto: track + knob render at 45% alpha.
 */
@Composable
fun TemperatureSlider(
    offset: Float,
    isAuto: Boolean = false,
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
    var isDragging by remember { mutableStateOf(false) }
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
            .height(36.dp)
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
                            val prev = lastTick.intValue
                            lastTick.intValue = detent
                            if ((prev < 0 && detent >= 0) || (prev > 0 && detent <= 0) || detent == 0) {
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
        val drawAlpha = if (isAuto) 0.45f else 1.0f

        Canvas(modifier = Modifier.matchParentSize()) {
            val trackH = ChromeTokens.TrackH.toPx() // 14 dp
            val trackTop = (size.height - trackH) / 2f
            val cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)

            // 1. Draw capsule gradient track
            drawRoundRect(
                brush = trackStyle.brush(),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackH),
                cornerRadius = cornerRadius,
                alpha = drawAlpha
            )

            // 2. Center detent notch (1 dp white 50% notch at 0 offset)
            val centerHairlineHeight = trackH - 4.dp.toPx()
            drawRect(
                color = Color.White.copy(alpha = 0.50f * drawAlpha),
                topLeft = Offset(size.width / 2f - 0.5.dp.toPx(), trackTop + 2.dp.toPx()),
                size = Size(1.dp.toPx(), centerHairlineHeight)
            )

            // 3. Specular track border
            drawRoundRect(
                color = Color.White.copy(alpha = 0.20f * drawAlpha),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackH),
                cornerRadius = cornerRadius,
                style = Stroke(width = 0.8.dp.toPx())
            )

            // 4. Black dot knob with 1.5 dp white ring stroke (18 dp diameter)
            val knobRadius = ChromeTokens.KnobD.toPx() / 2f
            val usableTravel = (size.width - 2f * knobRadius).coerceAtLeast(1f)
            val fraction = (currentKnobOffset + 100f) / 200f
            val knobX = knobRadius + fraction * usableTravel

            drawCircle(
                color = Color.Black.copy(alpha = drawAlpha),
                radius = knobRadius,
                center = Offset(knobX, size.height / 2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = drawAlpha),
                radius = knobRadius,
                center = Offset(knobX, size.height / 2f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
