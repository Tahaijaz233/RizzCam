package com.vintagecam.feature.viewfinder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vintagecam.feature.viewfinder.model.CameraBadgeType
import com.vintagecam.feature.viewfinder.model.CameraGraphicType
import com.vintagecam.feature.viewfinder.model.CameraModel

/**
 * Skeuomorphic miniature vintage camera rendering component matching the Dazz Cam reference designs.
 */
@Composable
fun CameraModelGraphic(
    model: CameraModel,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            when (model.graphicType) {
                CameraGraphicType.RANGEFINDER_SILVER -> drawRangefinder(w, h, isSilver = true)
                CameraGraphicType.RANGEFINDER_BLACK -> drawRangefinder(w, h, isSilver = false)
                CameraGraphicType.COMPACT_TITANIUM -> drawCompactTitanium(w, h)
                CameraGraphicType.COMPACT_CHAMPAGNE -> drawCompactChampagne(w, h)
                CameraGraphicType.COMPACT_BLUE -> drawCompactBlue(w, h)
                CameraGraphicType.DIGICAM_CCD -> drawDigicamCcd(w, h)
                CameraGraphicType.DIGICAM_BLACK -> drawDigicamBlack(w, h)
                CameraGraphicType.SMARTPHONE_PHOTO -> drawSmartphone(w, h, isVideo = false)
                CameraGraphicType.SMARTPHONE_VIDEO -> drawSmartphone(w, h, isVideo = true)
                CameraGraphicType.COLLAGE_FRAME -> drawCollageFrame(w, h)
                CameraGraphicType.STEREO_3D -> drawStereo3D(w, h)
                CameraGraphicType.FILM_NEGATIVE_STRIP -> drawFilmStrip(w, h)
                CameraGraphicType.TOY_CAMERA_YELLOW -> drawToyCameraYellow(w, h)
                CameraGraphicType.TOY_CAMERA_RED -> drawToyCameraRed(w, h)
                CameraGraphicType.INFRARED_RED_BLUE -> drawInfrared(w, h)
                CameraGraphicType.SLR_CLASSIC_SILVER -> drawSlr(w, h, isSilver = true)
                CameraGraphicType.SLR_CLASSIC_BLACK -> drawSlr(w, h, isSilver = false)
                CameraGraphicType.DISPOSABLE_GREEN -> drawDisposable(w, h, isGreen = true)
                CameraGraphicType.DISPOSABLE_NEON -> drawDisposable(w, h, isGreen = false)
                CameraGraphicType.MULTI_LENS_GOLF -> drawGolfCamera(w, h)
                CameraGraphicType.HALF_FRAME_PEN -> drawHalfFramePen(w, h)
                CameraGraphicType.SLIDE_FRAME -> drawSlideFrame(w, h)
                CameraGraphicType.TLR_ROLLEI -> drawTlr(w, h)
                CameraGraphicType.HOLGA_MEDIUM -> drawHolga(w, h)
                CameraGraphicType.PENTAX_67 -> drawPentax67(w, h)
                CameraGraphicType.HASSELBLAD_CUBE -> drawHasselbladCube(w, h)
                CameraGraphicType.INSTAX_WHITE -> drawInstaxWhite(w, h)
                CameraGraphicType.INSTAX_SQUARE_DARK -> drawInstaxSquare(w, h, isOrange = false)
                CameraGraphicType.INSTAX_SQUARE_ORANGE -> drawInstaxSquare(w, h, isOrange = true)
                CameraGraphicType.POLAROID_BELLOWS -> drawPolaroidBellows(w, h)
                CameraGraphicType.CINE_16MM -> drawCine16mm(w, h)
                CameraGraphicType.SUPER_8_YELLOW -> drawSuper8(w, h)
                CameraGraphicType.VHS_CAMCORDER -> drawVhs(w, h)
                CameraGraphicType.KINO_WHITE -> drawKino(w, h)
                CameraGraphicType.VIDEO_FUN_YELLOW -> drawVideoFunYellow(w, h)
                CameraGraphicType.HANDYCAM_GOLD -> drawHandycam(w, h)
                CameraGraphicType.CYBER_GLOW_PINK -> drawCyberGlow(w, h)
                CameraGraphicType.PROJECTOR_SLIDE -> drawProjectorSlide(w, h)
                CameraGraphicType.ACTION_CAM_DAM -> drawActionCamDam(w, h)
                CameraGraphicType.TEAL_VINTAGE -> drawTealVintage(w, h)
                CameraGraphicType.SPY_CAM_SILVER -> drawSpyCam(w, h)
                CameraGraphicType.PHOTO_BOOTH -> drawPhotoBooth(w, h)
                CameraGraphicType.SPORTS_YELLOW -> drawSportsYellow(w, h)
            }

            // Draw badges if applicable
            when (model.badge) {
                CameraBadgeType.BETA -> drawBetaBadge(w, h)
                CameraBadgeType.PLAY -> drawPlayBadge(w, h)
                else -> { /* No graphic badge */ }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Graphic Rendering Primitives
// -----------------------------------------------------------------------------

private fun DrawScope.drawRangefinder(w: Float, h: Float, isSilver: Boolean) {
    val topColor = if (isSilver) Color(0xFFD6D6DC) else Color(0xFF2C2C2E)
    val bodyColor = if (isSilver) Color(0xFF1E1E20) else Color(0xFF1A1A1A)

    // Main Body
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(w * 0.08f, h * 0.22f),
        size = Size(w * 0.84f, h * 0.62f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Top Plate
    drawRoundRect(
        color = topColor,
        topLeft = Offset(w * 0.08f, h * 0.22f),
        size = Size(w * 0.84f, h * 0.22f),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
    )
    // Rangefinder Window
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(w * 0.16f, h * 0.26f),
        size = Size(w * 0.18f, h * 0.12f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
    // Shutter Button
    drawCircle(
        color = Color(0xFFDC2626),
        radius = w * 0.045f,
        center = Offset(w * 0.22f, h * 0.20f)
    )
    // Central Lens Barrel
    val lensCenter = Offset(w * 0.52f, h * 0.54f)
    drawCircle(color = if (isSilver) Color(0xFFB0B0B8) else Color(0xFF333333), radius = w * 0.24f, center = lensCenter)
    drawCircle(color = Color(0xFF111827), radius = w * 0.19f, center = lensCenter)
    drawCircle(color = Color(0xFF1E3A8A), radius = w * 0.13f, center = lensCenter)
    drawCircle(color = Color(0x88FFFFFF), radius = w * 0.04f, center = lensCenter - Offset(w * 0.05f, w * 0.05f))
}

private fun DrawScope.drawCompactTitanium(w: Float, h: Float) {
    val champColor = Color(0xFFE2D6C5)
    drawRoundRect(
        color = champColor,
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.84f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
    )
    // Flash window
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(w * 0.70f, h * 0.30f),
        size = Size(w * 0.14f, h * 0.10f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
    // Lens
    val lc = Offset(w * 0.44f, h * 0.52f)
    drawCircle(color = Color(0xFFC7B79E), radius = w * 0.22f, center = lc)
    drawCircle(color = Color(0xFF18181B), radius = w * 0.17f, center = lc)
    drawCircle(color = Color(0xFF0284C7), radius = w * 0.09f, center = lc)
    drawCircle(color = Color(0x99FFFFFF), radius = w * 0.03f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawCompactChampagne(w: Float, h: Float) {
    // Clamshell Mju style
    val cColor = Color(0xFFEAE5DC)
    drawRoundRect(
        color = cColor,
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.84f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
    )
    // Lens
    val lc = Offset(w * 0.46f, h * 0.51f)
    drawCircle(color = Color(0xFF27272A), radius = w * 0.20f, center = lc)
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.14f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
    // Clamshell slider groove
    drawRoundRect(
        color = Color(0x44000000),
        topLeft = Offset(w * 0.72f, h * 0.32f),
        size = Size(w * 0.12f, h * 0.38f),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
    )
}

private fun DrawScope.drawCompactBlue(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFF0EA5E9),
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.84f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    val lc = Offset(w * 0.50f, h * 0.52f)
    drawCircle(color = Color(0xFF0284C7), radius = w * 0.22f, center = lc)
    drawCircle(color = Color(0xFF0369A1), radius = w * 0.17f, center = lc)
    drawCircle(color = Color(0xFF0C4A6E), radius = w * 0.12f, center = lc)
    drawCircle(color = Color(0x99FFFFFF), radius = w * 0.04f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawDigicamCcd(w: Float, h: Float) {
    // Silver Digicam with red 6.0 badge
    drawRoundRect(
        color = Color(0xFFE2E8F0),
        topLeft = Offset(w * 0.08f, h * 0.25f),
        size = Size(w * 0.84f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Red 6.0 badge
    drawRoundRect(
        color = Color(0xFFDC2626),
        topLeft = Offset(w * 0.14f, h * 0.30f),
        size = Size(w * 0.20f, h * 0.10f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
    // Telescoping lens barrel
    val lc = Offset(w * 0.58f, h * 0.52f)
    drawCircle(color = Color(0xFF94A3B8), radius = w * 0.22f, center = lc)
    drawCircle(color = Color(0xFF475569), radius = w * 0.16f, center = lc)
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.11f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawDigicamBlack(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.84f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    val lc = Offset(w * 0.50f, h * 0.52f)
    drawCircle(color = Color(0xFF2563EB), radius = w * 0.22f, center = lc)
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.16f, center = lc)
    drawCircle(color = Color(0x99FFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawSmartphone(w: Float, h: Float, isVideo: Boolean) {
    drawRoundRect(
        color = Color(0xFF27272A),
        topLeft = Offset(w * 0.22f, h * 0.12f),
        size = Size(w * 0.56f, h * 0.76f),
        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
    )
    // Camera Bump
    drawRoundRect(
        color = Color(0xFF18181B),
        topLeft = Offset(w * 0.26f, h * 0.16f),
        size = Size(w * 0.26f, h * 0.26f),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
    )
    // Lenses
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.06f, center = Offset(w * 0.34f, h * 0.22f))
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.06f, center = Offset(w * 0.34f, h * 0.34f))
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.05f, center = Offset(w * 0.44f, h * 0.28f))
}

private fun DrawScope.drawCollageFrame(w: Float, h: Float) {
    // 2 overlapping instant prints
    drawRoundRect(
        color = Color(0xFFF1F5F9),
        topLeft = Offset(w * 0.10f, h * 0.20f),
        size = Size(w * 0.44f, h * 0.60f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )
    drawRoundRect(
        color = Color(0xFF0EA5E9),
        topLeft = Offset(w * 0.14f, h * 0.24f),
        size = Size(w * 0.36f, h * 0.42f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
    // Second print rotated slightly
    drawRoundRect(
        color = Color(0xFFFFFFFF),
        topLeft = Offset(w * 0.44f, h * 0.24f),
        size = Size(w * 0.46f, h * 0.62f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )
    drawRoundRect(
        color = Color(0xFFF59E0B),
        topLeft = Offset(w * 0.49f, h * 0.29f),
        size = Size(w * 0.36f, h * 0.42f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
}

private fun DrawScope.drawStereo3D(w: Float, h: Float) {
    // Orange body with 3 distinct lenses
    drawRoundRect(
        color = Color(0xFFEA580C),
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.84f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
    )
    // Top 3D badge bar
    drawRoundRect(
        color = Color(0xFF0284C7),
        topLeft = Offset(w * 0.14f, h * 0.28f),
        size = Size(w * 0.30f, h * 0.10f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
    // 3 lenses
    val yCenter = h * 0.56f
    listOf(w * 0.26f, w * 0.50f, w * 0.74f).forEach { xCenter ->
        drawCircle(color = Color(0xFF18181B), radius = w * 0.10f, center = Offset(xCenter, yCenter))
        drawCircle(color = Color(0xFF0369A1), radius = w * 0.06f, center = Offset(xCenter, yCenter))
        drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.02f, center = Offset(xCenter - w * 0.02f, yCenter - w * 0.02f))
    }
}

private fun DrawScope.drawFilmStrip(w: Float, h: Float) {
    // Dark brown/amber negative strip with sprockets
    drawRoundRect(
        color = Color(0xFF1C1917),
        topLeft = Offset(w * 0.22f, h * 0.10f),
        size = Size(w * 0.56f, h * 0.80f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )
    // Center exposed image
    drawRoundRect(
        color = Color(0xFFB45309),
        topLeft = Offset(w * 0.30f, h * 0.22f),
        size = Size(w * 0.40f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
    // Sprockets on left & right
    listOf(h * 0.16f, h * 0.32f, h * 0.48f, h * 0.64f, h * 0.78f).forEach { yPos ->
        drawRoundRect(color = Color(0xFFF59E0B), topLeft = Offset(w * 0.24f, yPos), size = Size(w * 0.04f, h * 0.06f), cornerRadius = CornerRadius(2f, 2f))
        drawRoundRect(color = Color(0xFFF59E0B), topLeft = Offset(w * 0.72f, yPos), size = Size(w * 0.04f, h * 0.06f), cornerRadius = CornerRadius(2f, 2f))
    }
}

private fun DrawScope.drawToyCameraYellow(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFFFACC15),
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.84f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
    )
    // Red right side grip
    drawRoundRect(
        color = Color(0xFFDC2626),
        topLeft = Offset(w * 0.66f, h * 0.24f),
        size = Size(w * 0.26f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
    )
    // Lens
    val lc = Offset(w * 0.44f, h * 0.51f)
    drawCircle(color = Color(0xFF18181B), radius = w * 0.20f, center = lc)
    drawCircle(color = Color(0xFF475569), radius = w * 0.14f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawToyCameraRed(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFFEF4444),
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.84f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
    )
    val lc = Offset(w * 0.50f, h * 0.51f)
    drawCircle(color = Color(0xFF18181B), radius = w * 0.20f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawInfrared(w: Float, h: Float) {
    // Red & Cyan two-tone body
    drawRoundRect(
        color = Color(0xFF0284C7),
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.42f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    drawRoundRect(
        color = Color(0xFFDC2626),
        topLeft = Offset(w * 0.50f, h * 0.24f),
        size = Size(w * 0.42f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    val lc = Offset(w * 0.50f, h * 0.51f)
    drawCircle(color = Color(0xFFF8FAFC), radius = w * 0.22f, center = lc)
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.17f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawSlr(w: Float, h: Float, isSilver: Boolean) {
    val bodyColor = if (isSilver) Color(0xFF1E293B) else Color(0xFF0F172A)
    val topColor = if (isSilver) Color(0xFFCBD5E1) else Color(0xFF334155)

    // Base body
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(w * 0.08f, h * 0.28f),
        size = Size(w * 0.84f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Pentaprism top peak
    val prismPath = Path().apply {
        moveTo(w * 0.34f, h * 0.28f)
        lineTo(w * 0.50f, h * 0.14f)
        lineTo(w * 0.66f, h * 0.28f)
        close()
    }
    drawPath(path = prismPath, color = topColor)

    // Large lens barrel
    val lc = Offset(w * 0.50f, h * 0.55f)
    drawCircle(color = topColor, radius = w * 0.24f, center = lc)
    drawCircle(color = Color(0xFF0284C7), radius = w * 0.18f, center = lc)
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.13f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawDisposable(w: Float, h: Float, isGreen: Boolean) {
    val baseColor = if (isGreen) Color(0xFF15803D) else Color(0xFF84CC16)
    drawRoundRect(
        color = baseColor,
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.84f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Flash window
    drawRoundRect(
        color = Color(0xFFFEF08A),
        topLeft = Offset(w * 0.68f, h * 0.30f),
        size = Size(w * 0.16f, h * 0.12f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
    val lc = Offset(w * 0.44f, h * 0.51f)
    drawCircle(color = Color(0xFF1E293B), radius = w * 0.20f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawGolfCamera(w: Float, h: Float) {
    // Dark green body with 8 tiny circular lenses
    drawRoundRect(
        color = Color(0xFF064E3B),
        topLeft = Offset(w * 0.08f, h * 0.22f),
        size = Size(w * 0.84f, h * 0.58f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Top name badge "Golf"
    drawRoundRect(
        color = Color(0xFFF1F5F9),
        topLeft = Offset(w * 0.14f, h * 0.26f),
        size = Size(w * 0.26f, h * 0.10f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
    // 2 rows of 4 lenses
    val xPositions = listOf(w * 0.20f, w * 0.38f, w * 0.56f, w * 0.74f)
    val yPositions = listOf(h * 0.48f, h * 0.64f)
    for (y in yPositions) {
        for (x in xPositions) {
            drawCircle(color = Color(0xFF022C22), radius = w * 0.06f, center = Offset(x, y))
            drawCircle(color = Color(0xFF34D399), radius = w * 0.035f, center = Offset(x, y))
        }
    }
}

private fun DrawScope.drawHalfFramePen(w: Float, h: Float) {
    // White top, black textured bottom
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.84f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    drawRoundRect(
        color = Color(0xFFF8FAFC),
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.84f, h * 0.24f),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
    )
    val lc = Offset(w * 0.48f, h * 0.52f)
    drawCircle(color = Color(0xFFCBD5E1), radius = w * 0.20f, center = lc)
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.14f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawSlideFrame(w: Float, h: Float) {
    // White paper slide frame
    drawRoundRect(
        color = Color(0xFFF8FAFC),
        topLeft = Offset(w * 0.14f, h * 0.14f),
        size = Size(w * 0.72f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )
    // Film aperture
    drawRoundRect(
        color = Color(0xFFEA580C),
        topLeft = Offset(w * 0.24f, h * 0.26f),
        size = Size(w * 0.52f, h * 0.42f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
}

private fun DrawScope.drawTlr(w: Float, h: Float) {
    // Tall Rolleiflex vertical body with two lenses
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(w * 0.20f, h * 0.10f),
        size = Size(w * 0.60f, h * 0.80f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Viewing lens (top)
    val topLens = Offset(w * 0.50f, h * 0.34f)
    drawCircle(color = Color(0xFF94A3B8), radius = w * 0.18f, center = topLens)
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.13f, center = topLens)

    // Taking lens (bottom)
    val botLens = Offset(w * 0.50f, h * 0.64f)
    drawCircle(color = Color(0xFF94A3B8), radius = w * 0.18f, center = botLens)
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.13f, center = botLens)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = botLens - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawHolga(w: Float, h: Float) {
    // Black & red toy camera
    drawRoundRect(
        color = Color(0xFFDC2626),
        topLeft = Offset(w * 0.10f, h * 0.24f),
        size = Size(w * 0.80f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    val lc = Offset(w * 0.50f, h * 0.51f)
    drawCircle(color = Color(0xFF1E293B), radius = w * 0.22f, center = lc)
    drawCircle(color = Color(0xFF475569), radius = w * 0.16f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawPentax67(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFF111827),
        topLeft = Offset(w * 0.06f, h * 0.24f),
        size = Size(w * 0.88f, h * 0.58f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    val prismPath = Path().apply {
        moveTo(w * 0.30f, h * 0.24f)
        lineTo(w * 0.50f, h * 0.12f)
        lineTo(w * 0.70f, h * 0.24f)
        close()
    }
    drawPath(path = prismPath, color = Color(0xFF1F2937))
    val lc = Offset(w * 0.50f, h * 0.53f)
    drawCircle(color = Color(0xFF374151), radius = w * 0.26f, center = lc)
    drawCircle(color = Color(0xFF111827), radius = w * 0.19f, center = lc)
}

private fun DrawScope.drawHasselbladCube(w: Float, h: Float) {
    // Square cube with waist level finder
    drawRoundRect(
        color = Color(0xFF1F2937),
        topLeft = Offset(w * 0.18f, h * 0.22f),
        size = Size(w * 0.64f, h * 0.64f),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
    )
    val lc = Offset(w * 0.50f, h * 0.54f)
    drawCircle(color = Color(0xFF4B5563), radius = w * 0.24f, center = lc)
    drawCircle(color = Color(0xFF0284C7), radius = w * 0.17f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawInstaxWhite(w: Float, h: Float) {
    // Bulbous white body
    drawRoundRect(
        color = Color(0xFFF8FAFC),
        topLeft = Offset(w * 0.14f, h * 0.16f),
        size = Size(w * 0.72f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
    )
    // Red shutter button
    drawCircle(color = Color(0xFFEF4444), radius = w * 0.05f, center = Offset(w * 0.24f, h * 0.32f))
    val lc = Offset(w * 0.52f, h * 0.54f)
    drawCircle(color = Color(0xFFCBD5E1), radius = w * 0.22f, center = lc)
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.15f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawInstaxSquare(w: Float, h: Float, isOrange: Boolean) {
    val bColor = if (isOrange) Color(0xFFC2410C) else Color(0xFF1E293B)
    drawRoundRect(
        color = bColor,
        topLeft = Offset(w * 0.14f, h * 0.16f),
        size = Size(w * 0.72f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
    )
    val lc = Offset(w * 0.50f, h * 0.52f)
    drawCircle(color = Color(0xFFE2E8F0), radius = w * 0.22f, center = lc)
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.15f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawPolaroidBellows(w: Float, h: Float) {
    // Folding SX-70 with tan leather and black bellows
    drawRoundRect(
        color = Color(0xFF9A3412),
        topLeft = Offset(w * 0.14f, h * 0.26f),
        size = Size(w * 0.72f, h * 0.52f),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
    )
    // Front lens plate
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(w * 0.24f, h * 0.34f),
        size = Size(w * 0.52f, h * 0.40f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )
    val lc = Offset(w * 0.50f, h * 0.54f)
    drawCircle(color = Color(0xFFCBD5E1), radius = w * 0.18f, center = lc)
    drawCircle(color = Color(0xFF0284C7), radius = w * 0.12f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.03f, center = lc - Offset(w * 0.03f, w * 0.03f))
}

private fun DrawScope.drawCine16mm(w: Float, h: Float) {
    // Bolex style 16mm cine camera
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(w * 0.20f, h * 0.16f),
        size = Size(w * 0.54f, h * 0.68f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Red record dot / lens
    drawCircle(color = Color(0xFFDC2626), radius = w * 0.07f, center = Offset(w * 0.36f, h * 0.56f))
    // Side viewfinder tube
    drawRoundRect(color = Color(0xFF475569), topLeft = Offset(w * 0.74f, h * 0.36f), size = Size(w * 0.14f, h * 0.20f), cornerRadius = CornerRadius(4f, 4f))
}

private fun DrawScope.drawSuper8(w: Float, h: Float) {
    // Yellow Super 8 handheld with bold '8'
    drawRoundRect(
        color = Color(0xFFF59E0B),
        topLeft = Offset(w * 0.20f, h * 0.16f),
        size = Size(w * 0.56f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Bold '8' representation
    drawCircle(color = Color(0xFF1E293B), radius = w * 0.08f, center = Offset(w * 0.48f, h * 0.36f))
    drawCircle(color = Color(0xFFF59E0B), radius = w * 0.04f, center = Offset(w * 0.48f, h * 0.36f))
    drawCircle(color = Color(0xFF1E293B), radius = w * 0.09f, center = Offset(w * 0.48f, h * 0.52f))
    drawCircle(color = Color(0xFFF59E0B), radius = w * 0.05f, center = Offset(w * 0.48f, h * 0.52f))
    // Pistol grip
    drawRoundRect(color = Color(0xFF18181B), topLeft = Offset(w * 0.42f, h * 0.72f), size = Size(w * 0.16f, h * 0.20f), cornerRadius = CornerRadius(4f, 4f))
}

private fun DrawScope.drawVhs(w: Float, h: Float) {
    // Shoulder VHS camcorder
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(w * 0.10f, h * 0.24f),
        size = Size(w * 0.80f, h * 0.52f),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
    )
    // Tape door window
    drawRoundRect(
        color = Color(0xFF0284C7),
        topLeft = Offset(w * 0.22f, h * 0.38f),
        size = Size(w * 0.56f, h * 0.24f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
}

private fun DrawScope.drawKino(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFFF8FAFC),
        topLeft = Offset(w * 0.20f, h * 0.18f),
        size = Size(w * 0.60f, h * 0.64f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // Crank lever
    drawCircle(color = Color(0xFF94A3B8), radius = w * 0.12f, center = Offset(w * 0.50f, h * 0.50f))
}

private fun DrawScope.drawVideoFunYellow(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFFFACC15),
        topLeft = Offset(w * 0.12f, h * 0.20f),
        size = Size(w * 0.76f, h * 0.60f),
        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
    )
    drawCircle(color = Color(0xFFDC2626), radius = w * 0.14f, center = Offset(w * 0.56f, h * 0.56f))
}

private fun DrawScope.drawHandycam(w: Float, h: Float) {
    // Champagne gold Handycam
    drawRoundRect(
        color = Color(0xFFE2D6C5),
        topLeft = Offset(w * 0.12f, h * 0.22f),
        size = Size(w * 0.76f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
    )
    val lc = Offset(w * 0.64f, h * 0.50f)
    drawCircle(color = Color(0xFF1E293B), radius = w * 0.18f, center = lc)
}

private fun DrawScope.drawCyberGlow(w: Float, h: Float) {
    // Pearlescent purple/pink camcorder
    drawRoundRect(
        brush = Brush.linearGradient(listOf(Color(0xFFE879F9), Color(0xFFA855F7))),
        topLeft = Offset(w * 0.16f, h * 0.20f),
        size = Size(w * 0.68f, h * 0.60f),
        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
    )
    // Sparkle
    drawCircle(color = Color.White, radius = w * 0.04f, center = Offset(w * 0.40f, h * 0.36f))
}

private fun DrawScope.drawProjectorSlide(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFFF1F5F9),
        topLeft = Offset(w * 0.14f, h * 0.26f),
        size = Size(w * 0.72f, h * 0.50f),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
    )
    val lc = Offset(w * 0.50f, h * 0.50f)
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.18f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.03f, w * 0.03f))
}

private fun DrawScope.drawActionCamDam(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFF27272A),
        topLeft = Offset(w * 0.12f, h * 0.20f),
        size = Size(w * 0.76f, h * 0.60f),
        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
    )
    // Red record button dot
    drawCircle(color = Color(0xFFDC2626), radius = w * 0.045f, center = Offset(w * 0.32f, h * 0.36f))
    val lc = Offset(w * 0.60f, h * 0.50f)
    drawCircle(color = Color(0xFF0284C7), radius = w * 0.18f, center = lc)
}

private fun DrawScope.drawTealVintage(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFF0D9488),
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.84f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    val lc = Offset(w * 0.50f, h * 0.51f)
    drawCircle(color = Color(0xFF1E293B), radius = w * 0.20f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

private fun DrawScope.drawSpyCam(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFFCBD5E1),
        topLeft = Offset(w * 0.10f, h * 0.30f),
        size = Size(w * 0.80f, h * 0.40f),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
    )
    drawCircle(color = Color(0xFF1E293B), radius = w * 0.14f, center = Offset(w * 0.50f, h * 0.50f))
}

private fun DrawScope.drawPhotoBooth(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFFF8FAFC),
        topLeft = Offset(w * 0.16f, h * 0.14f),
        size = Size(w * 0.68f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    drawRoundRect(
        color = Color(0xFFEC4899),
        topLeft = Offset(w * 0.30f, h * 0.24f),
        size = Size(w * 0.40f, h * 0.52f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )
}

private fun DrawScope.drawSportsYellow(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFFEAB308),
        topLeft = Offset(w * 0.08f, h * 0.24f),
        size = Size(w * 0.84f, h * 0.54f),
        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
    )
    val lc = Offset(w * 0.50f, h * 0.51f)
    drawCircle(color = Color(0xFF0F172A), radius = w * 0.20f, center = lc)
    drawCircle(color = Color(0xAAFFFFFF), radius = w * 0.035f, center = lc - Offset(w * 0.04f, w * 0.04f))
}

// -----------------------------------------------------------------------------
// Badge Drawing
// -----------------------------------------------------------------------------

private fun DrawScope.drawBetaBadge(w: Float, h: Float) {
    val center = Offset(w * 0.16f, h * 0.80f)
    val radius = w * 0.12f
    drawCircle(color = Color(0xFF06B6D4), radius = radius, center = center)

    // Draw stylized beta 'β'
    val stroke = Stroke(width = 2.dp.toPx())
    val path = Path().apply {
        moveTo(center.x - radius * 0.3f, center.y + radius * 0.6f)
        lineTo(center.x - radius * 0.3f, center.y - radius * 0.6f)
        cubicTo(
            center.x + radius * 0.4f, center.y - radius * 0.6f,
            center.x + radius * 0.4f, center.y - radius * 0.05f,
            center.x - radius * 0.3f, center.y - radius * 0.05f
        )
        cubicTo(
            center.x + radius * 0.5f, center.y - radius * 0.05f,
            center.x + radius * 0.5f, center.y + radius * 0.55f,
            center.x - radius * 0.3f, center.y + radius * 0.55f
        )
    }
    drawPath(path = path, color = Color.White, style = stroke)
}

private fun DrawScope.drawPlayBadge(w: Float, h: Float) {
    val center = Offset(w * 0.16f, h * 0.80f)
    val radius = w * 0.12f
    drawCircle(color = Color(0xFFEF4444), radius = radius, center = center)

    // Draw white play triangle '▶'
    val path = Path().apply {
        moveTo(center.x - radius * 0.3f, center.y - radius * 0.45f)
        lineTo(center.x + radius * 0.45f, center.y)
        lineTo(center.x - radius * 0.3f, center.y + radius * 0.45f)
        close()
    }
    drawPath(path = path, color = Color.White, style = Fill)
}
