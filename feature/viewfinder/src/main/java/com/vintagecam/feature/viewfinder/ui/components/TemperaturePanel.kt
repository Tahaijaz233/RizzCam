package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vintagecam.core.common.model.WhiteBalanceState
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine

/**
 * Temperature editor panel forwarding to TemperatureEditorRow.
 */
@Composable
fun TemperaturePanel(
    wb: WhiteBalanceState,
    onOffsetChange: (Float) -> Unit,
    onCommitOffset: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    onAutoToggle: () -> Unit = {},
    onWarmStep: () -> Unit = {},
    onWarmRamp: () -> Unit = {},
    onCoolStep: () -> Unit = {},
    onCoolRamp: () -> Unit = {},
    trackStyle: TempTrackStyle = TempTrackStyle.SPECTRUM,
    haptics: RetroHapticsEngine? = null
) {
    TemperatureEditorRow(
        wb = wb,
        onOffsetChange = onOffsetChange,
        onCommitOffset = onCommitOffset,
        onReset = onReset,
        onAutoToggle = onAutoToggle,
        onWarmStep = onWarmStep,
        onWarmRamp = onWarmRamp,
        onCoolStep = onCoolStep,
        onCoolRamp = onCoolRamp,
        modifier = modifier,
        trackStyle = trackStyle,
        haptics = haptics
    )
}
