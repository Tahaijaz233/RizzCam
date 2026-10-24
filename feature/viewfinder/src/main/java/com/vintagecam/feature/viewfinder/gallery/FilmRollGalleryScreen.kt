package com.vintagecam.feature.viewfinder.gallery

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Filter categories for gallery media assets on the top bar.
 */
enum class GalleryCategory(val label: String) {
    ALL("All photos"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    OTHERS("Others")
}

/**
 * Media asset classification types.
 */
enum class MediaType {
    IMAGE,
    VIDEO,
    OTHER
}

/**
 * Representation of an individual media asset in the gallery.
 */
data class GalleryItem(
    val id: Long,
    val uri: Uri? = null,
    val displayName: String,
    val dateTaken: Long,
    val mediaType: MediaType = MediaType.IMAGE,
    val mimeType: String = "",
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val isCameraPhoto: Boolean = true,
    val filePath: String? = null,
    val mediaStoreId: Long = id
)

/**
 * Compatibility model for legacy references.
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
 * Finds the key (item ID) of the item positioned under [offset] in the [gridState].
 * Guaranteed accurate across scrolling and responsive layouts.
 */
internal fun findItemKeyAtOffset(gridState: LazyGridState, offset: Offset): Long? {
    val visibleItems = gridState.layoutInfo.visibleItemsInfo
    return visibleItems.firstOrNull { item ->
        val x = item.offset.x.toFloat()
        val y = item.offset.y.toFloat()
        val w = item.size.width.toFloat()
        val h = item.size.height.toFloat()
        offset.x in x..(x + w) && offset.y in y..(y + h)
    }?.key as? Long
}

/**
 * Modern High-Performance Image Gallery Screen for RizzCam.
 *
 * Implements:
 * 1. Image gallery UI replacing legacy film roll negative look.
 * 2. All photos displayed in strict 1:1 square tiles.
 * 3. Small camera icon on the bottom left of photo thumbnails and floating screen return button.
 * 4. Top category filter selector ("All photos", "Images", "Videos", "Others").
 * 5. Selection mode allowing single or multi-selection with Delete and Share capabilities.
 * 6. Slide-over multi-selection: smoothly drag finger across items to select them.
 * 7. Fullscreen photo viewer with pinch-to-zoom, pan, share, and delete.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilmRollGalleryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var items by remember { mutableStateOf<List<GalleryItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf(GalleryCategory.ALL) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var previewItem by remember { mutableStateOf<GalleryItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePendingIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingSystemDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingAndroid10Uri by remember { mutableStateOf<Uri?>(null) }

    val systemDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deletedIds = pendingSystemDeleteIds
            val a10Uri = pendingAndroid10Uri

            // Launch background IO work for Android 10 retry and physical file cleanup
            coroutineScope.launch(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && a10Uri != null) {
                    try {
                        context.contentResolver.delete(a10Uri, null, null)
                    } catch (e: Exception) {
                        Log.w("ImageGallery", "Android 10 post-approval delete failed: $a10Uri", e)
                    }
                }

                // Clean up files on disk if accessible
                items.filter { it.id in deletedIds }.forEach { item ->
                    item.filePath?.let { path ->
                        try {
                            val file = File(path)
                            if (file.exists()) file.delete()
                        } catch (_: Exception) {}
                    }
                }
            }

            items = items.filterNot { it.id in deletedIds }
            selectedIds = selectedIds - deletedIds
            if (selectedIds.isEmpty()) {
                isSelectionMode = false
            }
            if (previewItem?.id in deletedIds) {
                previewItem = null
            }
        }
        pendingSystemDeleteIds = emptySet()
        pendingAndroid10Uri = null
    }

    val gridState = rememberLazyGridState()
    var gridHeightPx by remember { mutableFloatStateOf(1000f) }
    val edgeScrollThreshold = with(density) { 90.dp.toPx() }
    val scrollStep = with(density) { 24.dp.toPx() }

    // Query media items on mount
    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            queryAllMedia(context)
        }
        items = loaded
    }

    // System back button handling
    BackHandler {
        when {
            previewItem != null -> previewItem = null
            isSelectionMode -> {
                isSelectionMode = false
                selectedIds = emptySet()
            }
            else -> onNavigateBack()
        }
    }

    // Filter media by active top category
    val filteredItems = remember(items, selectedCategory) {
        when (selectedCategory) {
            GalleryCategory.ALL -> items
            GalleryCategory.IMAGES -> items.filter { it.mediaType == MediaType.IMAGE }
            GalleryCategory.VIDEOS -> items.filter { it.mediaType == MediaType.VIDEO }
            GalleryCategory.OTHERS -> items.filter { it.mediaType == MediaType.OTHER || !it.isCameraPhoto }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            text = "${selectedIds.size} selected",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Gallery",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedIds = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Selection",
                                tint = Color.White
                            )
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Camera",
                                tint = Color.White
                            )
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Toggle Select All
                        IconButton(onClick = {
                            selectedIds = if (selectedIds.size == filteredItems.size) {
                                emptySet()
                            } else {
                                filteredItems.map { it.id }.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = Color(0xFFFFD54F)
                            )
                        }

                        // Share Action
                        IconButton(
                            onClick = {
                                val selectedItems = items.filter { it.id in selectedIds }
                                shareMediaItems(context, selectedItems)
                            },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = if (selectedIds.isNotEmpty()) Color.White else Color(0xFF666666)
                            )
                        }

                        // Delete Action
                        IconButton(
                            onClick = {
                                deletePendingIds = selectedIds
                                showDeleteDialog = true
                            },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = if (selectedIds.isNotEmpty()) Color(0xFFFF5252) else Color(0xFF666666)
                            )
                        }
                    } else {
                        // Enter selection mode button
                        TextButton(onClick = { isSelectionMode = true }) {
                            Text(
                                text = "Select",
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Category Selector Tabs on the top
                GalleryCategoryRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        selectedCategory = category
                    }
                )

                // Main Media Grid with Slide-Over Multi-Selection
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            gridHeightPx = size.height.toFloat()
                        }
                        .pointerInput(isSelectionMode, filteredItems) {
                            if (isSelectionMode) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val touchedId = findItemKeyAtOffset(gridState, offset)
                                        if (touchedId != null) {
                                            selectedIds = selectedIds + touchedId
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val touchedId = findItemKeyAtOffset(gridState, change.position)
                                        if (touchedId != null) {
                                            selectedIds = selectedIds + touchedId
                                        }

                                        // Auto-scroll when dragging near viewport boundary
                                        val y = change.position.y
                                        if (y < edgeScrollThreshold) {
                                            coroutineScope.launch {
                                                gridState.scrollBy(-scrollStep)
                                            }
                                        } else if (y > gridHeightPx - edgeScrollThreshold) {
                                            coroutineScope.launch {
                                                gridState.scrollBy(scrollStep)
                                            }
                                        }
                                    }
                                )
                            } else {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        val touchedId = findItemKeyAtOffset(gridState, offset)
                                        if (touchedId != null) {
                                            isSelectionMode = true
                                            selectedIds = setOf(touchedId)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val touchedId = findItemKeyAtOffset(gridState, change.position)
                                        if (touchedId != null) {
                                            selectedIds = selectedIds + touchedId
                                        }

                                        val y = change.position.y
                                        if (y < edgeScrollThreshold) {
                                            coroutineScope.launch {
                                                gridState.scrollBy(-scrollStep)
                                            }
                                        } else if (y > gridHeightPx - edgeScrollThreshold) {
                                            coroutineScope.launch {
                                                gridState.scrollBy(scrollStep)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    if (filteredItems.isEmpty()) {
                        GalleryEmptyState(selectedCategory = selectedCategory)
                    } else {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                val isSelected = item.id in selectedIds

                                GallerySquareThumbnail(
                                    item = item,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedIds = if (isSelected) {
                                                selectedIds - item.id
                                            } else {
                                                selectedIds + item.id
                                            }
                                        } else {
                                            previewItem = item
                                        }
                                    },
                                    onLongClick = {
                                        isSelectionMode = true
                                        selectedIds = selectedIds + item.id
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Small camera icon on the bottom left of the gallery screen (quick return to camera)
            FloatingActionButton(
                onClick = onNavigateBack,
                shape = CircleShape,
                containerColor = Color(0xFF1E1E22).copy(alpha = 0.92f),
                contentColor = Color(0xFFFFD54F),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = 16.dp)
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Return to Camera",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    // Fullscreen interactive photo inspector
    previewItem?.let { item ->
        FullscreenPhotoViewer(
            item = item,
            onDismiss = { previewItem = null },
            onShare = {
                shareMediaItems(context, listOf(item))
            },
            onDelete = {
                deletePendingIds = setOf(item.id)
                showDeleteDialog = true
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && deletePendingIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deletePendingIds = emptySet()
            },
            title = {
                Text(
                    text = "Delete ${deletePendingIds.size} item${if (deletePendingIds.size > 1) "s" else ""}?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This will permanently delete the selected photos or videos from your device storage.",
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idsToDelete = deletePendingIds
                        showDeleteDialog = false
                        deletePendingIds = emptySet()

                        coroutineScope.launch {
                            val itemsToDelete = items.filter { it.id in idsToDelete }
                            val directlyDeletedIds = mutableSetOf<Long>()
                            val needsPermissionUris = mutableListOf<Uri>()
                            val needsPermissionIds = mutableSetOf<Long>()
                            var recoverableIntentSender: IntentSender? = null
                            var recoverablePendingId: Long? = null
                            var recoverablePendingUri: Uri? = null

                            withContext(Dispatchers.IO) {
                                for (item in itemsToDelete) {
                                    var deleted = false

                                    val primaryUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        ContentUris.withAppendedId(
                                            if (item.mediaType == MediaType.VIDEO)
                                                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                                            else
                                                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                                            item.mediaStoreId
                                        )
                                    } else null

                                    val standardUri = item.uri

                                    // 1. Try deleting via primary volume URI first (where RizzCam exports photos on Android 10+)
                                    if (primaryUri != null) {
                                        try {
                                            val rows = context.contentResolver.delete(primaryUri, null, null)
                                            if (rows > 0) {
                                                deleted = true
                                            }
                                        } catch (e: SecurityException) {
                                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                                                if (recoverableIntentSender == null) {
                                                    recoverableIntentSender = e.userAction.actionIntent.intentSender
                                                    recoverablePendingId = item.id
                                                    recoverablePendingUri = primaryUri
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.w("ImageGallery", "Failed deleting via primaryUri: $primaryUri", e)
                                        }
                                    }

                                    // 2. If not deleted, try deleting via standard/external URI
                                    if (!deleted && standardUri != null) {
                                        try {
                                            val rows = context.contentResolver.delete(standardUri, null, null)
                                            if (rows > 0) {
                                                deleted = true
                                            }
                                        } catch (e: SecurityException) {
                                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                                                if (recoverableIntentSender == null) {
                                                    recoverableIntentSender = e.userAction.actionIntent.intentSender
                                                    recoverablePendingId = item.id
                                                    recoverablePendingUri = standardUri
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.w("ImageGallery", "Failed deleting via standardUri: $standardUri", e)
                                        }
                                    }

                                    // 3. Physical file deletion on disk (cleans up file and triggers scanner cleanup)
                                    item.filePath?.let { path ->
                                        try {
                                            val file = File(path)
                                            if (file.exists()) {
                                                val fileDeleted = file.delete()
                                                if (fileDeleted && !deleted) {
                                                    // File deleted from disk; try cleaning up MediaStore rows as well
                                                    try {
                                                        if (primaryUri != null) context.contentResolver.delete(primaryUri, null, null)
                                                    } catch (_: Exception) {}
                                                    try {
                                                        if (standardUri != null) context.contentResolver.delete(standardUri, null, null)
                                                    } catch (_: Exception) {}
                                                    deleted = true
                                                }
                                            }
                                            Unit
                                        } catch (e: Exception) {
                                            Log.w("ImageGallery", "Failed deleting file from disk: $path", e)
                                        }
                                    }

                                    if (deleted) {
                                        directlyDeletedIds.add(item.id)
                                    } else {
                                        // Deletion not completed directly: requires system permission dialog
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            val targetUri = standardUri ?: primaryUri
                                            if (targetUri != null) {
                                                needsPermissionUris.add(targetUri)
                                                needsPermissionIds.add(item.id)
                                            }
                                        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                                            val recId = recoverablePendingId
                                            if (recId != null) {
                                                needsPermissionIds.add(recId)
                                            }
                                        }
                                    }
                                }
                            }

                            // Remove items that succeeded directly
                            if (directlyDeletedIds.isNotEmpty()) {
                                items = items.filterNot { it.id in directlyDeletedIds }
                                selectedIds = selectedIds - directlyDeletedIds
                                if (selectedIds.isEmpty()) {
                                    isSelectionMode = false
                                }
                                if (previewItem?.id in directlyDeletedIds) {
                                    previewItem = null
                                }
                            }

                            // Launch system confirmation dialog for items requiring user consent
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && needsPermissionUris.isNotEmpty()) {
                                try {
                                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, needsPermissionUris)
                                    pendingSystemDeleteIds = needsPermissionIds
                                    pendingAndroid10Uri = null
                                    val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                    systemDeleteLauncher.launch(request)
                                } catch (e: Exception) {
                                    Log.e("ImageGallery", "Failed to launch MediaStore.createDeleteRequest", e)
                                }
                            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                                val sender = recoverableIntentSender
                                val recId = recoverablePendingId
                                if (sender != null) {
                                    pendingSystemDeleteIds = if (recId != null) setOf(recId) else emptySet()
                                    pendingAndroid10Uri = recoverablePendingUri
                                    val request = IntentSenderRequest.Builder(sender).build()
                                    systemDeleteLauncher.launch(request)
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deletePendingIds = emptySet()
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFFAAAAAA)
                    )
                }
            },
            containerColor = Color(0xFF1E1E22)
        )
    }
}

/**
 * Top category filter selector row.
 */
@Composable
private fun GalleryCategoryRow(
    selectedCategory: GalleryCategory,
    onCategorySelected: (GalleryCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GalleryCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) Color(0xFFFFD54F) else Color(0xFF202024),
                contentColor = if (isSelected) Color(0xFF121214) else Color(0xFFB0B0B0),
                modifier = Modifier
                    .clickable { onCategorySelected(category) }
            ) {
                Text(
                    text = category.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Square thumbnail item for the gallery grid.
 *
 * Implements:
 * - Square 1:1 aspect ratio constraint.
 * - Small camera icon badge on bottom-left.
 * - Video duration pill on bottom-right (for video assets).
 * - Multi-selection checkbox and border highlight.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GallerySquareThumbnail(
    item: GalleryItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f) // All photos stored and displayed as a square
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF1A1A1E))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) Color(0xFFFFD54F) else Color.Transparent,
                shape = RoundedCornerShape(3.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Thumbnail Image
        MemorySafeThumbnail(
            uri = item.uri,
            isVideo = item.mediaType == MediaType.VIDEO,
            contentDescription = item.displayName,
            modifier = Modifier.fillMaxSize()
        )

        // Small camera icon on the bottom left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = if (item.mediaType == MediaType.VIDEO) Icons.Default.Videocam else Icons.Default.CameraAlt,
                contentDescription = "Camera",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }

        // Video duration badge on bottom-right
        if (item.mediaType == MediaType.VIDEO && item.durationMs > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(item.durationMs),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        // Selection overlay and checkmark badge
        if (isSelectionMode) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x33FFD54F))
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(
                        if (isSelected) Color(0xFFFFD54F) else Color.Black.copy(alpha = 0.45f),
                        CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.8f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Memory-safe thumbnail composable that decodes using bounded sample size.
 */
@Composable
private fun MemorySafeThumbnail(
    uri: Uri?,
    isVideo: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        if (uri == null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val size = Size(360, 360)
                    context.contentResolver.loadThumbnail(uri, size, null)
                } else {
                    if (isVideo) {
                        null
                    } else {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val options = BitmapFactory.Options().apply {
                                inSampleSize = 2
                            }
                            BitmapFactory.decodeStream(stream, null, options)
                        }
                    }
                }
            } catch (e: Exception) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 2
                        }
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                } catch (t: Throwable) {
                    null
                }
            }
        }
        bitmap = loaded
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
                .background(Color(0xFF1B1B1E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color(0xFF444448),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Interactive full-screen photo viewer with pinch-to-zoom and pan gestures.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullscreenPhotoViewer(
    item: GalleryItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var fullBitmap by remember(item.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.uri) {
        val uri = item.uri ?: return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 1
                    }
                    BitmapFactory.decodeStream(stream, null, options)
                }
            } catch (e: Exception) {
                null
            }
        }
        fullBitmap = loaded
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = item.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatDate(item.dateTaken),
                            fontSize = 11.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF5252)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.85f)
                )
            )
        },
        containerColor = Color.Black,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale == 1f) {
                            offset = Offset.Zero
                        } else {
                            val maxOffsetX = (size.width * (scale - 1)) / 2f
                            val maxOffsetY = (size.height * (scale - 1)) / 2f
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val bmp = fullBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = item.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            } else {
                Text(
                    text = "Loading photo...",
                    color = Color(0xFF888888),
                    fontSize = 14.sp
                )
            }

            // Zoom reset pill
            if (scale > 1f) {
                Surface(
                    color = Color.Black.copy(alpha = 0.70f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Double tap to reset zoom (${String.format(Locale.US, "%.1f", scale)}x)",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
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
    }
}

/**
 * Clean empty state placeholder.
 */
@Composable
private fun GalleryEmptyState(
    selectedCategory: GalleryCategory,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = Color(0xFF44444A),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No ${selectedCategory.label.lowercase()} found",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCCCCCC)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Photos captured with RizzCam are stored as square photos and will appear here.",
                fontSize = 13.sp,
                color = Color(0xFF777777),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Launches Android system share sheet for single or multiple media assets.
 */
private fun shareMediaItems(context: Context, itemsToShare: List<GalleryItem>) {
    if (itemsToShare.isEmpty()) return
    try {
        val intent = if (itemsToShare.size == 1) {
            val item = itemsToShare.first()
            val mime = item.mimeType.ifEmpty {
                if (item.mediaType == MediaType.VIDEO) "video/*" else "image/*"
            }
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                item.uri?.let { putExtra(Intent.EXTRA_STREAM, it) }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val hasVideo = itemsToShare.any { it.mediaType == MediaType.VIDEO }
            val hasImage = itemsToShare.any { it.mediaType == MediaType.IMAGE }
            val mime = when {
                hasVideo && !hasImage -> "video/*"
                hasImage && !hasVideo -> "image/*"
                else -> "*/*"
            }
            val validUris = ArrayList(itemsToShare.mapNotNull { it.uri })
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mime
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    validUris
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(intent, "Share via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Log.e("ImageGallery", "Error launching share sheet", e)
    }
}

/**
 * Formats duration in milliseconds to M:SS representation.
 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

/**
 * Formats timestamp to readable date string.
 */
private fun formatDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}

/**
 * Queries device MediaStore for images and videos.
 */
private fun queryAllMedia(context: Context): List<GalleryItem> {
    val result = mutableListOf<GalleryItem>()
    val contentResolver = context.contentResolver

    // 1. Query Images
    try {
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.MediaColumns.DATA
        )
        val imageSelection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Images.Media.SIZE} > 0 AND ${MediaStore.MediaColumns.IS_PENDING} = 0"
        } else {
            "${MediaStore.Images.Media.SIZE} > 0"
        }
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageProjection,
            imageSelection,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "IMAGE_$id.JPG"
                val date = cursor.getLong(dateCol)
                val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                val size = cursor.getLong(sizeCol)
                if (size <= 0) continue

                val path = if (dataCol != -1 && !cursor.isNull(dataCol)) cursor.getString(dataCol) else null
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                // Verify file actually exists if path is accessible; cleanup dangling MediaStore rows if missing
                if (path != null) {
                    try {
                        val file = File(path)
                        if (!file.exists()) {
                            try {
                                contentResolver.delete(uri, null, null)
                            } catch (_: Exception) {}
                            continue
                        }
                    } catch (_: Exception) {}
                }

                val isCamera = name.contains("RIZZ", ignoreCase = true) ||
                        name.contains("IMG", ignoreCase = true) ||
                        name.contains("DCIM", ignoreCase = true)

                val mediaType = if (mime.startsWith("image/")) {
                    MediaType.IMAGE
                } else {
                    MediaType.OTHER
                }

                result.add(
                    GalleryItem(
                        id = id,
                        uri = uri,
                        displayName = name,
                        dateTaken = date,
                        mediaType = mediaType,
                        mimeType = mime,
                        sizeBytes = size,
                        isCameraPhoto = isCamera,
                        filePath = path,
                        mediaStoreId = id
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.w("ImageGallery", "Failed querying images", e)
    }

    // 2. Query Videos
    try {
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.MediaColumns.DATA
        )
        val videoSelection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Video.Media.SIZE} > 0 AND ${MediaStore.MediaColumns.IS_PENDING} = 0"
        } else {
            "${MediaStore.Video.Media.SIZE} > 0"
        }
        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            videoSelection,
            null,
            "${MediaStore.Video.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "VIDEO_$id.MP4"
                val date = cursor.getLong(dateCol)
                val mime = cursor.getString(mimeCol) ?: "video/mp4"
                val dur = cursor.getLong(durCol)
                val size = cursor.getLong(sizeCol)
                if (size <= 0) continue

                val path = if (dataCol != -1 && !cursor.isNull(dataCol)) cursor.getString(dataCol) else null
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                if (path != null) {
                    try {
                        val file = File(path)
                        if (!file.exists()) {
                            try {
                                contentResolver.delete(uri, null, null)
                            } catch (_: Exception) {}
                            continue
                        }
                    } catch (_: Exception) {}
                }

                result.add(
                    GalleryItem(
                        id = id + 100_000_000L,
                        uri = uri,
                        displayName = name,
                        dateTaken = date,
                        mediaType = MediaType.VIDEO,
                        mimeType = mime,
                        durationMs = dur,
                        sizeBytes = size,
                        isCameraPhoto = true,
                        filePath = path,
                        mediaStoreId = id
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.w("ImageGallery", "Failed querying videos", e)
    }

    return result.sortedByDescending { it.dateTaken }
}
