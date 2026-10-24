package com.vintagecam.core.camera.timestamp

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Authentic visual styles for retro timestamps.
 */
enum class TimestampStyle(val displayName: String) {
    AUTO("Auto (Matches Camera)"),
    DIGICAM_ORANGE("Orange Quartz LED (Digicam)"),
    CAMCORDER_GREEN("Phosphor Green (Camcorder OSD)"),
    RETRO_GOLD("Golden Quartz (Film Back)")
}

/**
 * Common date display formats from classic 90s/2000s quartz date backs and camcorders.
 */
enum class TimestampFormat(val displayName: String) {
    YEAR_MONTH_DAY(" 'YY MM DD (Classic Digicam)"),
    MONTH_DAY_YEAR("MM DD 'YY (US Format)"),
    DATE_WITH_TIME(" 'YY MM DD  HH:mm (With Time)"),
    CAMCORDER_OSD("MMM. DD YYYY  HH:mm a (Camcorder)")
}

/**
 * High-fidelity retro digital timestamp renderer.
 *
 * Implements authentic characteristics of:
 * 1. Vintage CCD Digicams & 35mm Quartz Date Backs (Olympus Stylus, Canon PowerShot, Pentax Data Back):
 *    - Glowing amber-orange LED numerals (`#FF7A00` / `#FF9000`).
 *    - Warm golden inner filament highlight (`#FFDE59`).
 *    - Dual-pass dark drop-shadow and stroke outline for 100% legibility on stark white backgrounds.
 *    - Monospaced wide tracking simulating physical quartz LED displays.
 * 2. Retro Camcorders & VHS/Hi8/DV OSDs:
 *    - Glowing phosphor green numerals (`#22C55E` / `#00FF66`).
 *    - Characteristic VCR monospace uppercase format (`OCT. 24 1998  12:30 PM`).
 *    - Subtle raster outline.
 */
object RetroTimestampRenderer {

    /**
     * Formats a given date according to the requested [TimestampFormat].
     */
    fun formatTimestamp(date: Date, format: TimestampFormat): String {
        val cal = Calendar.getInstance()
        cal.time = date

        val yearFull = cal.get(Calendar.YEAR)
        val year2Digit = String.format(Locale.US, "%02d", yearFull % 100)
        val month2Digit = String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)
        val day2Digit = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))
        val hour2Digit = String.format(Locale.US, "%02d", cal.get(Calendar.HOUR_OF_DAY))
        val minute2Digit = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))

        return when (format) {
            TimestampFormat.YEAR_MONTH_DAY -> "'$year2Digit $month2Digit $day2Digit"
            TimestampFormat.MONTH_DAY_YEAR -> "$month2Digit $day2Digit '$year2Digit"
            TimestampFormat.DATE_WITH_TIME -> "'$year2Digit $month2Digit $day2Digit  $hour2Digit:$minute2Digit"
            TimestampFormat.CAMCORDER_OSD -> {
                val monthName = SimpleDateFormat("MMM", Locale.US).format(date).uppercase(Locale.US)
                val timeAmPm = SimpleDateFormat("hh:mm a", Locale.US).format(date).uppercase(Locale.US)
                "$monthName. $day2Digit $yearFull  $timeAmPm"
            }
        }
    }

    /**
     * Determines the resolved visual style given user preference and camera type.
     */
    fun resolveStyle(requestedStyle: TimestampStyle, isVideoPreset: Boolean): TimestampStyle {
        return if (requestedStyle == TimestampStyle.AUTO) {
            if (isVideoPreset) TimestampStyle.CAMCORDER_GREEN else TimestampStyle.DIGICAM_ORANGE
        } else {
            requestedStyle
        }
    }

    /**
     * Renders and stamps the retro date onto [sourceBitmap].
     *
     * @param sourceBitmap The input image to imprint.
     * @param style Visual style (Orange quartz, phosphor green, or golden).
     * @param format Date/time format.
     * @param date Timestamp value (defaults to current system time).
     * @param isVideoPreset True if the camera is a camcorder/video preset.
     * @return Stamped bitmap (mutates in-place if mutable, or returns a new copy).
     */
    fun applyTimestamp(
        sourceBitmap: Bitmap,
        style: TimestampStyle = TimestampStyle.AUTO,
        format: TimestampFormat = TimestampFormat.YEAR_MONTH_DAY,
        date: Date = Date(),
        isVideoPreset: Boolean = false
    ): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        if (width <= 0 || height <= 0) return sourceBitmap

        val workingBitmap = if (sourceBitmap.isMutable) {
            sourceBitmap
        } else {
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val canvas = Canvas(workingBitmap)
        val resolvedStyle = resolveStyle(style, isVideoPreset)

        val resolvedFormat = if (resolvedStyle == TimestampStyle.CAMCORDER_GREEN && format == TimestampFormat.YEAR_MONTH_DAY) {
            TimestampFormat.CAMCORDER_OSD
        } else {
            format
        }

        val text = formatTimestamp(date, resolvedFormat)

        // Proportionate sizing: ~3.2% of frame height (authentic to 35mm quartz date back window)
        val fontSize = (height * 0.032f).coerceAtLeast(18f)
        val marginRight = width * 0.045f
        val marginBottom = height * 0.045f

        val typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = fontSize
            this.letterSpacing = if (resolvedStyle == TimestampStyle.CAMCORDER_GREEN) 0.08f else 0.16f
        }

        val textWidth = basePaint.measureText(text)
        val fontMetrics = basePaint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent

        val x = (width - textWidth - marginRight).coerceAtLeast(marginRight)
        val y = height - marginBottom - fontMetrics.descent

        when (resolvedStyle) {
            TimestampStyle.CAMCORDER_GREEN -> {
                // 1. Black Drop Shadow for high contrast on white/bright video
                val shadowPaint = Paint(basePaint).apply {
                    color = Color.BLACK
                }
                canvas.drawText(text, x + fontSize * 0.06f, y + fontSize * 0.06f, shadowPaint)

                // 2. Phosphor Green Diffuse Bloom
                val glowPaint = Paint(basePaint).apply {
                    color = Color.argb(160, 0, 255, 100)
                    maskFilter = BlurMaskFilter(fontSize * 0.20f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawText(text, x, y, glowPaint)

                // 3. Crisp Phosphor Green Core
                val corePaint = Paint(basePaint).apply {
                    color = Color.rgb(40, 255, 110) // #28FF6E
                }
                canvas.drawText(text, x, y, corePaint)
            }

            TimestampStyle.RETRO_GOLD -> {
                // 1. Black stroke outline for contrast
                val strokePaint = Paint(basePaint).apply {
                    color = Color.argb(200, 0, 0, 0)
                    this.style = Paint.Style.STROKE
                    strokeWidth = fontSize * 0.10f
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawText(text, x, y, strokePaint)

                // 2. Golden Core
                val corePaint = Paint(basePaint).apply {
                    color = Color.rgb(255, 185, 0) // #FFB900
                }
                canvas.drawText(text, x, y, corePaint)

                // 3. Bright Highlight
                val highlightPaint = Paint(basePaint).apply {
                    color = Color.rgb(255, 235, 130)
                }
                canvas.drawText(text, x, y, highlightPaint)
            }

            TimestampStyle.DIGICAM_ORANGE,
            TimestampStyle.AUTO -> {
                // 1. Black stroke outline for 100% legibility on stark white backgrounds
                val strokePaint = Paint(basePaint).apply {
                    color = Color.argb(220, 10, 5, 0)
                    this.style = Paint.Style.STROKE
                    strokeWidth = fontSize * 0.12f
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawText(text, x, y, strokePaint)

                // 2. Dark Drop Shadow
                val shadowPaint = Paint(basePaint).apply {
                    color = Color.argb(180, 0, 0, 0)
                }
                canvas.drawText(text, x + fontSize * 0.05f, y + fontSize * 0.05f, shadowPaint)

                // 3. Diffuse Amber/Orange Glow Bloom
                val glowPaint = Paint(basePaint).apply {
                    color = Color.argb(150, 255, 80, 0)
                    maskFilter = BlurMaskFilter(fontSize * 0.22f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawText(text, x, y, glowPaint)

                // 4. Core Quartz Amber-Orange Body
                val corePaint = Paint(basePaint).apply {
                    color = Color.rgb(255, 125, 0) // #FF7D00 Safety/Quartz Orange
                }
                canvas.drawText(text, x, y, corePaint)

                // 5. Warm Golden Inner Filament Highlight
                val filamentPaint = Paint(basePaint).apply {
                    color = Color.rgb(255, 222, 90) // #FFDE5A Inner glowing filament
                    textSize = fontSize * 0.94f
                }
                val filamentWidth = filamentPaint.measureText(text)
                val filamentX = x + (textWidth - filamentWidth) / 2f
                canvas.drawText(text, filamentX, y - fontSize * 0.02f, filamentPaint)
            }
        }

        return workingBitmap
    }
}
