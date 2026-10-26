package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vintagecam.core.common.math.WhiteBalanceMath
import com.vintagecam.core.common.model.WhiteBalanceState
import com.vintagecam.feature.viewfinder.theme.ChromeTokens

/**
 * R5 Temperature Editor Panel:
 * - Live Kelvin & offset readout: e.g. "5103K −9"
 * - Capsule gradient track with black knob and tactile haptic detents
 */
@Composable
fun TemperaturePanel(
    wb: WhiteBalanceState,
    onOffsetChange: (Float) -> Unit,
    onCommitOffset: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    trackStyle: TempTrackStyle = TempTrackStyle.SPECTRUM
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        // Caption label: Kelvin + signed offset, white 60%, 11 sp, monospace
        val signedOffset = WhiteBalanceMath.formatSigned(wb.offsetDisplay)
        Text(
            text = "${wb.kelvinLabel}K $signedOffset",
            style = ChromeTokens.LabelMono,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        // Capsule gradient slider track with black knob
        TemperatureSlider(
            offset = wb.offset,
            onOffsetChange = onOffsetChange,
            onDragEnd = onCommitOffset,
            onReset = onReset,
            trackStyle = trackStyle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
