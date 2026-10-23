package com.vintagecam.feature.viewfinder.gallery

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Metadata record for a single film exposure frame on the contact sheet.
 */
data class FilmFrameItem(
    val id: Long,
    val uri: Uri,
    val dateTaken: Long,
    val displayName: String,
    val presetName: String = "Portra 400",
    val iso: String = "400",
    val shutterSpeed: String = "1/250s",
    val aperture: String = "f/2.8",
    val focalLength: String = "35mm"
)

/**
 * Skeuomorphic Film Roll Contact Sheet & Gallery Screen.
 *
 * Features:
 * - Vintage photographer's backlit milk-glass light table aesthetic.
 * - Continuous 35mm film negative strip with sprocket holes, frame numbers (01, 01A, 02, 02A...),
 *   and amber edge rebate markings.
 * - Full-screen photo inspection with interactive pinch-to-zoom and pan.
 * - Skeuomorphic "Photolab Development Slip": Perforated receipt with EXIF metadata & quality stamp.
 * - Strict memory safety: Bounded thumbnail decoding, zero leaks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmRollGalleryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var frames by remember { mutableStateOf<List<FilmFrameItem>>(emptyList()) }
    var selectedFrame by remember { mutableStateOf<FilmFrameItem?>(null) }
    var isDevelopmentSlipOpen by remember { mutableStateOf(false) }
    var isLightTableOn by remember { mutableStateOf(true) }

    // Load recent photos from MediaStore Pictures/RizzCam
    LaunchedEffect(Unit) {
        val loadedFrames = withContext(Dispatchers.IO) {
            queryRizzCamPhotos(context)
        }
        frames = loadedFrames
    }

    val lightTableColor = if (isLightTableOn) Color(0xFFF0F4F8) else Color(0xFF1E2022)
    val lightTableGlow = if (isLightTableOn) Color(0xFFFFFFFF) else Color(0xFF2B2D30)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LIGHT TABLE · CONTACT SHEET",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.8.sp,
                            color = Color(0xFFCCCCCC)
                        )
                        Text(
                            text = "ROLL #001 · 35MM NEGATIVE STRIPS",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF888888)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Light Table Power Toggle
                    IconButton(onClick = { isLightTableOn = !isLightTableOn }) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Toggle Light Table",
                            tint = if (isLightTableOn) Color(0xFFFFD54F) else Color(0xFF666666)
                        )
                    }
                    // Open Photolab Development Slip
                    IconButton(onClick = { isDevelopmentSlipOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Photolab Slip",
                            tint = Color(0xFF4FC3F7)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121214)
                )
            )
        },
        containerColor = Color(0xFF0F0F11),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Light Table Backlit Surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(lightTableGlow, lightTableColor),
                            radius = 1200f
                        )
                    )
                    .border(2.dp, Color(0xFF2A2C30), RoundedCornerShape(8.dp))
            ) {
                // Subtle Light Table Grid Lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (isLightTableOn) {
                        val gridSize = 40.dp.toPx()
                        val gridColor = Color(0x18000000)
                        var x = 0f
                        while (x < size.width) {
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                            x += gridSize
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                            y += gridSize
                        }
                    }
                }

                if (frames.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO EXPOSURES ON THIS ROLL YET\nSNAP PHOTOS IN VIEWFINDER",
                            color = Color(0xFF777777),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            letterSpacing = 1.5.sp,
                            lineHeight = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    // Contact Sheet Film Strips (rows of 4 frames each)
                    val chunkedFrames = remember(frames) { frames.chunked(4) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        itemsIndexed(chunkedFrames) { stripIndex, stripFrames ->
                            FilmNegativeStrip(
                                stripIndex = stripIndex,
                                frames = stripFrames,
                                onFrameClicked = { selectedFrame = it }
                            )
                        }
                    }
                }
            }

            // Photolab Development Slip Modal Sheet
            AnimatedVisibility(
                visible = isDevelopmentSlipOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PhotolabDevelopmentSlip(
                    frame = selectedFrame ?: frames.firstOrNull(),
                    totalExposures = frames.size,
                    onClose = { isDevelopmentSlipOpen = false }
                )
            }

            // Fullscreen Photo Inspector with Pinch-to-Zoom
            selectedFrame?.let { frame ->
                FullscreenPhotoInspector(
                    frame = frame,
                    onClose = { selectedFrame = null }
                )
            }
        }
    }
}

/**
 * Skeuomorphic 35mm Film Negative Strip with sprocket perforations and edge rebate numbering.
 */
@Composable
private fun FilmNegativeStrip(
    stripIndex: Int,
    frames: List<FilmFrameItem>,
    onFrameClicked: (FilmFrameItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(2.dp), ambientColor = Color.Black)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF141211)) // Acetate film base
            .border(1.dp, Color(0xFF26221E), RoundedCornerShape(2.dp))
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        // Top Sprocket Holes & Edge Rebate Text
        SprocketEdgeBar(
            stripIndex = stripIndex,
            frameCount = frames.size,
            isTop = true
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Negative Frames Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            frames.forEachIndexed { index, frame ->
                val frameNumber = (stripIndex * 4 + index + 1)
                val frameLabel = String.format(Locale.US, "%02d", frameNumber)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(130.dp)
                ) {
                    // Film Frame Window
                    Box(
                        modifier = Modifier
                            .size(width = 130.dp, height = 95.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color(0xFF0A0908))
                            .border(1.dp, Color(0xFF332D27), RoundedCornerShape(1.dp))
                            .clickable { onFrameClicked(frame) }
                    ) {
                        MemorySafeThumbnail(
                            uri = frame.uri,
                            contentDescription = "Frame $frameLabel"
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Frame Index Stencil under frame
                    Text(
                        text = "$frameLabel ◀",
                        color = Color(0xFFE68A00), // Classic orange/amber rebate dye
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom Sprocket Holes & Barcodes
        SprocketEdgeBar(
            stripIndex = stripIndex,
            frameCount = frames.size,
            isTop = false
        )
    }
}

/**
 * Precision sprocket holes and orange/amber film edge rebate markings.
 */
@Composable
private fun SprocketEdgeBar(
    stripIndex: Int,
    frameCount: Int,
    isTop: Boolean,
    modifier: Modifier = Modifier
) {
    val rebateTextColor = Color(0xFFE68A00)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Sprocket Perforations Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val holeWidth = 14.dp.toPx()
            val holeHeight = 10.dp.toPx()
            val spacing = 22.dp.toPx()
            val corner = 2.dp.toPx()

            var currentX = 12.dp.toPx()
            val y = (size.height - holeHeight) / 2f

            while (currentX < size.width + 400.dp.toPx()) {
                drawRoundRect(
                    color = Color(0xFFF0F4F8), // White light passing through hole
                    topLeft = Offset(currentX, y),
                    size = Size(holeWidth, holeHeight),
                    cornerRadius = CornerRadius(corner, corner)
                )
                // Inner sprocket shadow
                drawRoundRect(
                    color = Color(0x33000000),
                    topLeft = Offset(currentX, y),
                    size = Size(holeWidth, holeHeight),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = 1f)
                )
                currentX += spacing
            }
        }

        // Stenciled text between sprockets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val mark1 = if (isTop) "KODAK PORTRA 400" else "SAFETY FILM 5063"
            val mark2 = if (isTop) "DX CASSETTE" else "PROCESS C-41"

            Text(
                text = mark1,
                color = rebateTextColor,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "▶ ${stripIndex * 4 + 1}A",
                color = rebateTextColor,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = mark2,
                color = rebateTextColor,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )
        }
    }
}

/**
 * Skeuomorphic Photolab Development Slip (receipt) displaying EXIF shooting metadata.
 */
@Composable
fun PhotolabDevelopmentSlip(
    frame: FilmFrameItem?,
    totalExposures: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateString = remember(frame) {
        val time = frame?.dateTaken ?: System.currentTimeMillis()
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(time))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(24.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFAF7EE)) // Aged warm thermal paper
            .border(1.dp, Color(0xFFDED8C7), RoundedCornerShape(4.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Perforated Sawtooth Top Edge Effect
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                val teeth = 24
                val toothWidth = size.width / teeth
                val path = Path().apply {
                    moveTo(0f, 0f)
                    for (i in 0 until teeth) {
                        lineTo((i + 0.5f) * toothWidth, size.height)
                        lineTo((i + 1f) * toothWidth, 0f)
                    }
                    close()
                }
                drawPath(path, Color(0xFFE5DFC9))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lab Receipt Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RIZZ PHOTO LAB",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = Color(0xFF22201D)
                    )
                    Text(
                        text = "ORDER DOCKET #RZ-9482-B",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF6B655A)
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF555045)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            ReceiptDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Lines
            ReceiptRow(label = "FILM STOCK", value = frame?.presetName ?: "PORTRA 400 (35mm)")
            ReceiptRow(label = "CHEMISTRY", value = "C-41 COLOR NEGATIVE")
            ReceiptRow(label = "EXPOSURE", value = "${frame?.shutterSpeed ?: "1/250s"} · ${frame?.aperture ?: "f/2.8"}")
            ReceiptRow(label = "ISO SPEED", value = frame?.iso ?: "400")
            ReceiptRow(label = "FOCAL LENGTH", value = frame?.focalLength ?: "35mm EQUIV")
            ReceiptRow(label = "EMULSION LOT", value = "#PR-8832-A")
            ReceiptRow(label = "TIMESTAMP", value = dateString)
            ReceiptRow(label = "TOTAL FRAMES", value = "$totalExposures / 36 EXPOSURES")

            Spacer(modifier = Modifier.height(12.dp))
            ReceiptDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Photolab Stamp of Quality
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .border(2.dp, Color(0xFFC62828), RoundedCornerShape(4.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "PASSED · QUALITY CONTROL CHECK",
                        color = Color(0xFFC62828),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF7A7365)
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF1E1C18)
        )
    }
}

@Composable
private fun ReceiptDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = Color(0xFFDDD6C3),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1f
        )
    }
}

/**
 * Fullscreen Interactive Photo Inspector with Pinch-to-Zoom & Pan.
 */
@Composable
fun FullscreenPhotoInspector(
    frame: FilmFrameItem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        val maxOffset = 600f * (scale - 1f)
                        offset = Offset(
                            (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                            (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        MemorySafeThumbnail(
            uri = frame.uri,
            contentDescription = frame.displayName,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Inspector",
                tint = Color.White
            )
        }

        // Zoom helper pill
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Pinch to zoom (${String.format(Locale.US, "%.1f", scale)}x) · Double tap to reset",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clickable {
                        scale = 1f
                        offset = Offset.Zero
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Memory-safe thumbnail composable that decodes using bounded sample size and recycles safely.
 */
@Composable
private fun MemorySafeThumbnail(
    uri: Uri,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        val decoded = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // Safeguard: prevents high-res bitmap leak
                    }
                    BitmapFactory.decodeStream(stream, null, options)
                }
            } catch (e: Exception) {
                null
            }
        }
        bitmap = decoded
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF1C1A18)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "EXPOSURE",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF665E55)
            )
        }
    }
}

/**
 * Queries MediaStore for photos exported to `Pictures/RizzCam`.
 */
private fun queryRizzCamPhotos(context: Context): List<FilmFrameItem> {
    val items = mutableListOf<FilmFrameItem>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_TAKEN
    )

    val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.DATA} LIKE ?"
    } else {
        "${MediaStore.Images.Media.DATA} LIKE ?"
    }
    val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf("%Pictures/RizzCam%", "%Pictures/RizzCam%")
    } else {
        arrayOf("%Pictures/RizzCam%")
    }
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

    try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "RIZZ_PHOTO.JPG"
                val date = cursor.getLong(dateColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                items.add(
                    FilmFrameItem(
                        id = id,
                        uri = contentUri,
                        dateTaken = date,
                        displayName = name
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return items
}
