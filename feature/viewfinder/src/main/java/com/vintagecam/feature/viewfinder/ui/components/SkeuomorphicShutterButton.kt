package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vintagecam.feature.viewfinder.haptics.RetroHapticsEngine
import com.vintagecam.feature.viewfinder.theme.RetroTheme
import kotlinx.coroutines.launch

/**
 * Skeuomorphic mechanical shutter actuation button.
 *
 * Modeled after classic mechanical rangefinders (Leica M3 / Contax T2):
 * - Concentric brushed titanium and chrome collar bezel with radial highlight relief.
 * - Concave dish button face in high-gloss mechanical red (#D32F2F) with specular illumination.
 * - Threaded central mechanical cable-release socket with knurled brass spiral detailing.
 * - Physical spring travel physics: button physically depresses inward on touch down and springs back.
 * - Integrates [RetroHapticsEngine.shutterClick] for instant physical confirmation.
 */
@Composable
fun SkeuomorphicShutterButton(
    onClick: () -> Unit,
    haptics: RetroHapticsEngine,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    size: Dp = 86.dp
) {
    val coroutineScope = rememberCoroutineScope()
    val depressionProgress = remember { Animatable(0f) }

    val colors = RetroTheme.colors

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch {
                            haptics.shutterClick()
                            depressionProgress.animateTo(
                                targetValue = 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessHigh
                                )
                            )
                        }
                        val released = tryAwaitRelease()
                        coroutineScope.launch {
                            depressionProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            if (released) {
                                onClick()
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = (this.size.width / 2f) - 3.dp.toPx()
            val progress = depressionProgress.value

            // 1. Base Drop Shadow
            drawCircle(
                color = colors.chassisShadow.copy(alpha = 0.85f),
                radius = outerRadius,
                center = center + Offset(0f, 4.dp.toPx() * (1f - progress * 0.6f))
            )

            // 2. Outer Brushed Metal Collar Ring (Fixed bezel)
            val bezelBrush = Brush.sweepGradient(
                colors = listOf(
                    colors.chromeHighlight,
                    colors.chromeShadow,
                    colors.titaniumDark,
                    colors.chromeHighlight,
                    colors.chromeShadow,
                    colors.chromeHighlight
                ),
                center = center
            )
            drawCircle(
                brush = bezelBrush,
                radius = outerRadius,
                center = center
            )

            // Outer bezel lip highlight
            drawCircle(
                color = colors.chromeHighlight.copy(alpha = 0.6f),
                radius = outerRadius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 3. Knurled Collar Texture Ring
            val knurlRadius = outerRadius - 5.dp.toPx()
            drawCircle(
                color = colors.knurledGroove,
                radius = knurlRadius,
                center = center
            )
            drawCircle(
                color = colors.knurledBrass.copy(alpha = 0.45f),
                radius = knurlRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 4. Recessed Well Dark Shadow (Creates deep socket depth)
            val socketRadius = outerRadius - 10.dp.toPx()
            drawCircle(
                color = Color(0xFF09090A),
                radius = socketRadius,
                center = center
            )

            // -------------------------------------------------------------
            // 5. Concave Moving Red Shutter Button Core
            // -------------------------------------------------------------
            val buttonRadius = socketRadius - 2.dp.toPx() * (1f + progress * 0.08f)
            val buttonCenter = center + Offset(0f, 3.dp.toPx() * progress)

            // Concave Dish Lighting Gradient: specular light at top-left, deep crimson at rim
            val specularOffset = buttonCenter - Offset(buttonRadius * 0.28f, buttonRadius * 0.28f)
            val concaveBrush = Brush.radialGradient(
                colors = if (isEnabled) {
                    listOf(
                        colors.shutterRedHighlight,
                        colors.shutterRed,
                        colors.shutterRedPressed,
                        Color(0xFF6B0F0F)
                    )
                } else {
                    listOf(
                        Color(0xFF7A4A4A),
                        Color(0xFF5A3030),
                        Color(0xFF381C1C)
                    )
                },
                center = specularOffset,
                radius = buttonRadius * 1.25f
            )

            drawCircle(
                brush = concaveBrush,
                radius = buttonRadius,
                center = buttonCenter
            )

            // Concave inner rim shadow (creates sunken dish contour)
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f + progress * 0.25f),
                radius = buttonRadius - 1.dp.toPx(),
                center = buttonCenter,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // -------------------------------------------------------------
            // 6. Threaded Mechanical Cable Release Socket at Center
            // -------------------------------------------------------------
            val socketCenterRadius = 7.dp.toPx()
            // Socket outer brass chamfer
            drawCircle(
                color = colors.knurledBrass,
                radius = socketCenterRadius,
                center = buttonCenter
            )
            // Socket interior bore
            drawCircle(
                color = colors.shutterSocketInner,
                radius = socketCenterRadius - 1.5.dp.toPx(),
                center = buttonCenter
            )
            // Cable thread groove rings
            drawCircle(
                color = colors.brassHighlight.copy(alpha = 0.7f),
                radius = socketCenterRadius - 2.5.dp.toPx(),
                center = buttonCenter,
                style = Stroke(width = 0.8.dp.toPx())
            )
            drawCircle(
                color = Color.Black,
                radius = socketCenterRadius - 4.dp.toPx(),
                center = buttonCenter
            )
        }
    }
}
