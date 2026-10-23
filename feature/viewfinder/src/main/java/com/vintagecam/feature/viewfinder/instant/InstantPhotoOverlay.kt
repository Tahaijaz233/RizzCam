package com.vintagecam.feature.viewfinder.instant

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Skeuomorphic Polaroid SX-70 Instant Photo Overlay with:
 * - Physical slide-up ejection animation from the camera's bottom slot.
 * - Dynamic 3.0-second chemical emulsion development simulation (milky cyan opacifier clearing to vivid dyes).
 * - Interactive flick-to-dismiss physics gesture (drag, dynamic tilt, inertia throw).
 * - Strict memory safety: decodes Uri with bounded memory budget, recycles on dispose.
 */
@Composable
fun InstantPhotoOverlay(
    photoBitmap: Bitmap? = null,
    photoUri: Uri? = null,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Memory-safe bitmap loading for Uri with automatic recycling
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var userOwnedBitmap by remember { mutableStateOf(false) }

    DisposableEffect(photoUri, photoBitmap) {
        if (photoBitmap != null) {
            loadedBitmap = photoBitmap
            userOwnedBitmap = true
        } else if (photoUri != null) {
            try {
                context.contentResolver.openInputStream(photoUri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // Memory budget safeguard for high-res stills
                    }
                    loadedBitmap = BitmapFactory.decodeStream(stream, null, options)
                    userOwnedBitmap = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            if (!userOwnedBitmap) {
                loadedBitmap?.recycle()
                loadedBitmap = null
            }
        }
    }

    // 1. Physical Ejection Slide-Up Animation
    val ejectionOffsetY = remember { Animatable(1000f) }

    LaunchedEffect(Unit) {
        ejectionOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    // 2. Chemical Development Simulation (0.0 to 1.0 over 3000ms)
    val chemProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        chemProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 3000,
                easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
            )
        )
    }

    // 3. Interactive Flick-to-Dismiss Gesture & Dynamic Tilt
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }
    val velocityTracker = remember { VelocityTracker() }

    val rotationAngle = (dragOffsetX.value * 0.04f).coerceIn(-25f, 25f)
    val cardScale = if (abs(dragOffsetX.value) > 10f || abs(dragOffsetY.value) > 10f) 1.03f else 1.0f

    val dismissThreshold = with(density) { 160.dp.toPx() }
    val velocityThreshold = 1200f

    val dateFormatted = remember {
        SimpleDateFormat("yyyy.MM.dd", Locale.US).format(Date())
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { velocityTracker.resetTracking() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        coroutineScope.launch {
                            dragOffsetX.snapTo(dragOffsetX.value + dragAmount.x)
                            dragOffsetY.snapTo(dragOffsetY.value + dragAmount.y)
                        }
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity()
                        val speed = Offset(velocity.x, velocity.y).getDistance()

                        val isFlicked = abs(dragOffsetX.value) > dismissThreshold ||
                                abs(dragOffsetY.value) > dismissThreshold ||
                                speed > velocityThreshold

                        coroutineScope.launch {
                            if (isFlicked) {
                                val targetX = if (dragOffsetX.value >= 0) 1500f else -1500f
                                val targetY = dragOffsetY.value + velocity.y * 0.2f
                                launch { dragOffsetX.animateTo(targetX, tween(250)) }
                                launch { dragOffsetY.animateTo(targetY, tween(250)) }
                                onDismissed()
                            } else {
                                launch {
                                    dragOffsetX.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                    )
                                }
                                launch {
                                    dragOffsetY.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                    )
                                }
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Floating Instant Photo Card
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        dragOffsetX.value.roundToInt(),
                        (ejectionOffsetY.value + dragOffsetY.value).roundToInt()
                    )
                }
                .rotate(rotationAngle)
                .scale(cardScale)
                .width(310.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(6.dp),
                    ambientColor = Color.Black.copy(alpha = 0.6f),
                    spotColor = Color.Black.copy(alpha = 0.8f)
                )
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF7F5EF)) // Classic SX-70 eggshell cardstock
                .border(1.dp, Color(0xFFE2DFD2), RoundedCornerShape(6.dp))
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Photo Image Container (Recessed 1:1 square print)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.0f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF141716)) // Dark cavity before exposure
                        .border(1.dp, Color(0x33000000), RoundedCornerShape(2.dp))
                ) {
                    val currentBitmap = loadedBitmap
                    val p = chemProgress.value

                    if (currentBitmap != null) {
                        // S-curve color matrix: contrast & saturation emerge over time
                        val contrastFactor = 0.35f + 0.65f * p
                        val saturationFactor = (p * 1.2f).coerceIn(0.1f, 1.0f)

                        val matrix = ColorMatrix().apply {
                            setToSaturation(saturationFactor)
                            // Scale contrast
                            val t = (1.0f - contrastFactor) / 2.0f * 255f
                            timesAssign(
                                ColorMatrix(
                                    floatArrayOf(
                                        contrastFactor, 0f, 0f, 0f, t,
                                        0f, contrastFactor, 0f, 0f, t,
                                        0f, 0f, contrastFactor, 0f, t,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            )
                        }

                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = "Polaroid Instant Print",
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.colorMatrix(matrix),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Chemical Opacifier Emulsion Layer (Milky Cyan clearing from bottom pod)
                    if (p < 0.99f) {
                        val opacifierAlpha = ((1.0f - p) * 1.05f).coerceIn(0.0f, 0.96f)

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Authentic SX-70 reagent gradient: pod ruptured at bottom, clears upwards
                            val opacifierBrush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF6B8E89).copy(alpha = opacifierAlpha),
                                    Color(0xFF7EA69F).copy(alpha = opacifierAlpha * 0.92f),
                                    Color(0xFF98BCB6).copy(alpha = opacifierAlpha * 0.85f)
                                ),
                                startY = 0f,
                                endY = size.height
                            )
                            drawRect(brush = opacifierBrush)

                            // Subtle milky reagent diffusion swirl lines
                            if (p < 0.7f) {
                                val swirlAlpha = (0.7f - p) * 0.4f
                                drawCircle(
                                    color = Color(0xFFAFD3CD).copy(alpha = swirlAlpha),
                                    radius = size.width * 0.35f,
                                    center = Offset(size.width * 0.5f, size.height * (0.8f - p * 0.4f))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SX-70 Bottom Wide Margin "Chin" with authentic embossed typography
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "POLAROID SX-70",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.5.sp,
                            color = Color(0xFF888478)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = dateFormatted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFFA5A093)
                        )
                    }
                }
            }

            // Quick Dismiss Button
            IconButton(
                onClick = onDismissed,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Polaroid",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Hint at bottom
        Text(
            text = "Flick to dismiss",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
