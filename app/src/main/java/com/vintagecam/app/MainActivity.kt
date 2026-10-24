package com.vintagecam.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vintagecam.feature.viewfinder.gallery.FilmRollGalleryScreen
import com.vintagecam.feature.viewfinder.ui.ViewfinderScreen
import com.vintagecam.feature.viewfinder.viewmodel.ViewfinderViewModel

/**
 * Screen destinations for top-level navigation.
 */
sealed interface AppScreen {
    data object Viewfinder : AppScreen
    data object FilmRollGallery : AppScreen
}

/**
 * Edge-to-edge host Activity for RizzCam.
 *
 * Responsibilities:
 * - Edge-to-edge rendering with full system bar transparency.
 * - Runtime permission handling (Camera, Audio, Storage).
 * - Animated screen routing between Viewfinder and Film Roll Gallery.
 * - Hardware volume rocker shutter interception for authentic mechanical feel.
 */
class MainActivity : ComponentActivity() {

    private val viewfinderViewModel: ViewfinderViewModel by viewModels {
        ViewfinderViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            RizzCamApp(viewModel = viewfinderViewModel)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Physical shutter release via volume buttons
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            viewfinderViewModel.takePhoto()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun RizzCamApp(
    viewModel: ViewfinderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Viewfinder) }
    var permissionsGranted by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissions.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        permissions.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val cameraGranted = results[Manifest.permission.CAMERA] == true
        permissionsGranted = cameraGranted
    }

    LaunchedEffect(Unit) {
        val allGranted = requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            permissionsGranted = true
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F11))
    ) {
        if (!permissionsGranted) {
            PermissionRequestScreen(
                onRequestPermissions = {
                    permissionLauncher.launch(requiredPermissions)
                }
            )
        } else {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.Viewfinder -> {
                        ViewfinderScreen(
                            viewModel = viewModel,
                            onNavigateToGallery = {
                                currentScreen = AppScreen.FilmRollGallery
                            }
                        )
                    }
                    AppScreen.FilmRollGallery -> {
                        FilmRollGalleryScreen(
                            onNavigateBack = {
                                currentScreen = AppScreen.Viewfinder
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Skeuomorphic retro camera permission gate screen.
 */
@Composable
private fun PermissionRequestScreen(
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141312))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF332D28), RoundedCornerShape(12.dp))
                .background(Color(0xFF1C1A18), RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "RIZZCAM · ANALOG SYSTEM",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                color = Color(0xFFFFB300)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "OPTICAL SHUTTER LOCKED\n\nTo operate the vintage camera viewfinder, record Super 8 retro film video, and develop instant SX-70 prints to your film roll, hardware camera, audio, and media storage permissions are required.",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFFCCCCCC)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300),
                    contentColor = Color(0xFF111111)
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "INITIALIZE CAMERA",
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
