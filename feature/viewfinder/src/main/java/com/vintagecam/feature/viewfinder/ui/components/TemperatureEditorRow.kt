package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vintagecam.core.common.math.WhiteBalanceMath
import com.vintagecam.core.common.model.WhiteBalanceState
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.theme.ChromeTokens
import com.vintagecam.feature.viewfinder.theme.Motion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Master Temperature Editor Row matching reference:
 * `Row { Column(label, track).weight(1f) ; spacer 14 dp ; A ; sun ; bulb }`
 *
 * T2: Monospaced caption, white 60%, 11 sp, above the track, left-aligned to track start:
 *     "<Kelvin>K <signed offset>" e.g. "5103K −9". When auto: "AUTO".
 * T3: Gradient capsule slider (14 dp) + knob (18 dp black dot with 1.5 dp white ring).
 * T4: Button trio beside track: 44 dp circles with 10 dp spacing.
 *     - 'A': Auto white balance toggle
 *     - Sun: Warm / yellow push (+10 tap, +30/s hold-ramp)
 *     - Bulb: Cool / blue push (-10 tap, -30/s hold-ramp)
 */
@Composable
fun TemperatureEditorRow(
    wb: WhiteBalanceState,
    onOffsetChange: (Float) -> Unit,
    onCommitOffset: () -> Unit,
    onReset: () -> Unit,
    onAutoToggle: () -> Unit,
    onWarmStep: () -> Unit,
    onWarmRamp: () -> Unit,
    onCoolStep: () -> Unit,
    onCoolRamp: () -> Unit,
    modifier: Modifier = Modifier,
    trackStyle: TempTrackStyle = TempTrackStyle.SPECTRUM,
    haptics: RetroHapticsEngine? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Monospaced Label + Gradient Slider Track (weight 1f)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // T2: Monospaced caption above track, left-aligned to track start
            val labelText = if (wb.isAuto) {
                "AUTO"
            } else {
                val signedOffset = WhiteBalanceMath.formatSigned(wb.offsetDisplay)
                "${wb.kelvinLabel}K $signedOffset"
            }

            Text(
                text = labelText,
                style = ChromeTokens.LabelMono,
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
            )

            // T3: Gradient track + knob
            TemperatureSlider(
                offset = wb.offset,
                isAuto = wb.isAuto,
                onOffsetChange = onOffsetChange,
                onDragEnd = onCommitOffset,
                onReset = onReset,
                trackStyle = trackStyle,
                haptics = haptics,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // T4: Button trio beside track: [ A ] [ Sun ] [ Bulb ]
        // 1. 'A' Button (Auto White Balance Toggle)
        TempCircleButton(
            onClick = onAutoToggle,
            active = wb.isAuto,
            a11yLabel = "Auto white balance"
        ) {
            Text(
                text = "A",
                color = if (wb.isAuto) Color.White else Color.White.copy(alpha = 0.65f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 2. Sun Button (Warm / Yellow Push)
        val isWarmActive = !wb.isAuto && wb.offset > 0f
        TempRampingCircleButton(
            onTap = onWarmStep,
            onRamp = onWarmRamp,
            onRelease = onCommitOffset,
            active = isWarmActive,
            a11yLabel = "Warmer, yellow tone"
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = if (isWarmActive) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 3. Bulb Button (Cool / Blue Push)
        val isCoolActive = !wb.isAuto && wb.offset < 0f
        TempRampingCircleButton(
            onTap = onCoolStep,
            onRamp = onCoolRamp,
            onRelease = onCommitOffset,
            active = isCoolActive,
            a11yLabel = "Cooler, blue tone"
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = if (isCoolActive) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 44 dp circular button for discrete actions ('A' auto-toggle).
 * Inactive fill: white 14%. Active fill: white 28% + 1 dp white 45% ring.
 * Sticky press scale: 1.0 -> 0.88 -> spring back.
 */
@Composable
fun TempCircleButton(
    onClick: () -> Unit,
    active: Boolean,
    a11yLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val currentOnClick by rememberUpdatedState(onClick)

    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .semantics {
                this.role = Role.Button
                this.contentDescription = a11yLabel
            }
            .clip(CircleShape)
            .background(if (active) ChromeTokens.CircleFillActive else ChromeTokens.CircleFill)
            .then(
                if (active) Modifier.border(1.dp, ChromeTokens.ActiveRingColor, CircleShape)
                else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch {
                            scale.animateTo(0.88f, Motion.PressIn)
                        }
                        val released = tryAwaitRelease()
                        coroutineScope.launch {
                            scale.animateTo(1.0f, Motion.PressOut)
                        }
                        if (released) {
                            currentOnClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * 44 dp circular button with tap step and press-and-hold repeating ramp (Sun & Bulb).
 * Inactive fill: white 14%. Active fill: white 28% + 1 dp white 45% ring.
 * Sticky press scale: 1.0 -> 0.88 -> spring back.
 * Hold-ramp stops immediately on pointer-up/exit/cancel.
 */
@Composable
fun TempRampingCircleButton(
    onTap: () -> Unit,
    onRamp: () -> Unit,
    onRelease: () -> Unit,
    active: Boolean,
    a11yLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnRamp by rememberUpdatedState(onRamp)
    val currentOnRelease by rememberUpdatedState(onRelease)

    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .semantics {
                this.role = Role.Button
                this.contentDescription = a11yLabel
            }
            .clip(CircleShape)
            .background(if (active) ChromeTokens.CircleFillActive else ChromeTokens.CircleFill)
            .then(
                if (active) Modifier.border(1.dp, ChromeTokens.ActiveRingColor, CircleShape)
                else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch {
                            scale.animateTo(0.88f, Motion.PressIn)
                        }
                        var isHolding = false
                        val rampJob = coroutineScope.launch {
                            delay(300)
                            isHolding = true
                            while (true) {
                                currentOnRamp()
                                delay(50)
                            }
                        }
                        val released = tryAwaitRelease()
                        rampJob.cancel()
                        coroutineScope.launch {
                            scale.animateTo(1.0f, Motion.PressOut)
                        }
                        if (released && !isHolding) {
                            currentOnTap()
                        }
                        currentOnRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
