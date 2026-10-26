package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vintagecam.core.camera.model.FlashMode
import com.vintagecam.core.common.math.WhiteBalanceMath
import com.vintagecam.core.common.model.WhiteBalanceState
import com.vintagecam.feature.viewfinder.R
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.theme.ChromeTokens
import com.vintagecam.feature.viewfinder.theme.Motion
import com.vintagecam.feature.viewfinder.theme.glassPill
import kotlinx.coroutines.launch

/**
 * 44 dp circular chrome button matching iPhone / Dazz camera skeuomorphic circular buttons (R6).
 * Features sticky press scale physics (1.0 -> 0.88 -> spring back), active outline ring, and badge.
 */
@Composable
fun ChromeCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    active: Boolean = false,
    badge: String? = null,
    label: String = "",
    haptics: RetroHapticsEngine? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val effectiveHaptics = haptics ?: remember(context) { RetroHapticsEngine(context) }
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .semantics {
                this.role = Role.Button
                if (label.isNotEmpty()) {
                    this.contentDescription = label
                }
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
                    },
                    onLongPress = {
                        val longClick = currentOnLongClick
                        if (longClick != null) {
                            effectiveHaptics.heavyClick()
                            longClick()
                        }
                    },
                    onTap = {
                        effectiveHaptics.click()
                        currentOnClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()

        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .background(ChromeTokens.PillDarkFill, CircleShape)
                    .border(ChromeTokens.Hairline, ChromeTokens.HairlineTop, CircleShape)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Three 44 dp circular chrome buttons row (R6):
 * 1. 'A' (Auto-WB toggle)
 * 2. Thermometer (toggles R5 editor open/close; long-press resets)
 * 3. Bulb (Flash mode cycle)
 */
@Composable
fun CircleButtonRow(
    isAutoWb: Boolean,
    isTempPanelOpen: Boolean,
    wb: WhiteBalanceState,
    flashMode: FlashMode,
    onAutoWbClick: () -> Unit,
    onThermometerClick: () -> Unit,
    onThermometerReset: () -> Unit,
    onFlashToggle: () -> Unit,
    modifier: Modifier = Modifier,
    haptics: RetroHapticsEngine? = null
) {
    val context = LocalContext.current
    val effectiveHaptics = haptics ?: remember(context) { RetroHapticsEngine(context) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Auto-WB toggle circle ('A')
        ChromeCircleButton(
            onClick = onAutoWbClick,
            active = isAutoWb,
            label = stringResource(R.string.a11y_auto_wb),
            haptics = effectiveHaptics
        ) {
            Text(
                text = "A",
                color = if (isAutoWb) Color.White else Color.White.copy(alpha = 0.65f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.width(28.dp))

        // 2. Thermometer button (Primary feature)
        val isThermostatActive = isTempPanelOpen || !isAutoWb || wb.offsetDisplay != 0
        val tempBadge = if (!isAutoWb && wb.offsetDisplay != 0) {
            WhiteBalanceMath.formatSigned(wb.offsetDisplay)
        } else null

        ChromeCircleButton(
            onClick = onThermometerClick,
            onLongClick = onThermometerReset,
            active = isThermostatActive,
            badge = tempBadge,
            label = stringResource(R.string.a11y_temperature),
            haptics = effectiveHaptics
        ) {
            Icon(
                imageVector = Icons.Default.DeviceThermostat,
                contentDescription = null,
                tint = if (isThermostatActive) Color.White else Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(28.dp))

        // 3. Bulb (Flash) button
        val isFlashActive = flashMode != FlashMode.OFF
        ChromeCircleButton(
            onClick = onFlashToggle,
            active = isFlashActive,
            label = stringResource(R.string.a11y_flash),
            haptics = effectiveHaptics
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = if (isFlashActive) Color(0xFFFACC15) else Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Overlay control row positioned over the bottom of the viewfinder preview (R4):
 * - Left: Chevron button (collapses/expands temperature panel)
 * - Center: Frame counter (e.g. "26")
 * - Right: Exposure glass pill (e.g. "[ ☼ −1 ]") + persistent WB pill if offset != 0
 */
@Composable
fun OverlayControlRow(
    isPanelOpen: Boolean,
    onTogglePanel: () -> Unit,
    framesLeft: Int,
    exposureEv: Float,
    onExposureClick: () -> Unit,
    wbState: WhiteBalanceState,
    onWbPillClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Chevron-down / up collapse button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onTogglePanel() }
                .semantics {
                    role = Role.Button
                    contentDescription = "Toggle temperature panel"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPanelOpen) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Center: Plain monospace frame counter matching reference "26"
        Text(
            text = "$framesLeft",
            color = Color.White.copy(alpha = 0.85f),
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        // Right: Glass pills group (Exposure pill + optional WB badge pill)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Persistent WB glass pill when offset != 0 and panel is closed
            if (!isPanelOpen && wbState.offsetDisplay != 0) {
                Box(
                    modifier = Modifier
                        .glassPill(dark = false)
                        .clickable { onWbPillClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeviceThermostat,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = WhiteBalanceMath.formatSigned(wbState.offsetDisplay),
                            style = ChromeTokens.PillText
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Exposure glass pill: [ ☼ −1 ]
            val evInt = (exposureEv).toInt()
            val evFormatted = if (evInt.toFloat() == exposureEv) {
                WhiteBalanceMath.formatSigned(evInt)
            } else {
                val sign = if (exposureEv > 0f) "+" else if (exposureEv < 0f) "−" else ""
                val absVal = kotlin.math.abs(exposureEv)
                "$sign${String.format(java.util.Locale.US, "%.1f", absVal)}"
            }

            Box(
                modifier = Modifier
                    .glassPill(dark = false)
                    .clickable { onExposureClick() }
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = stringResource(R.string.a11y_exposure),
                        tint = Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = evFormatted,
                        style = ChromeTokens.PillText
                    )
                }
            }
        }
    }
}
