package com.vintagecam.domain.presets.usecase

import android.content.Context
import com.vintagecam.domain.presets.model.CameraPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encapsulates the binary and image assets required to render a vintage camera preset.
 */
data class LoadedPresetAssets(
    val preset: CameraPreset,
    val lutDirectBuffer: ByteBuffer,
    val lightLeaks: Map<String, ByteArray>,
    val borderMaskBytes: ByteArray?
)

/**
 * Suspended use case responsible for loading all binary assets (3D LUT, light leaks, border masks)
 * for a specified [CameraPreset] from Android assets on [Dispatchers.IO].
 *
 * Designed to ensure lean runtime RAM usage (< 10 MB in RAM for active preset assets)
 * and direct buffer compatibility for zero-copy GL texture uploads.
 */
class LoadPresetAssetsUseCase(
    private val context: Context
) {
    /**
     * Loads the raw assets for [preset].
     * @param preset The camera preset containing asset paths for LUT and overlays.
     * @return [LoadedPresetAssets] containing direct byte buffers and raw image bytes.
     */
    suspend fun execute(preset: CameraPreset): LoadedPresetAssets = withContext(Dispatchers.IO) {
        val assetManager = context.assets

        // 1. Load 3D LUT binary (.raw3d)
        // 64x64x64 * 3 bytes = 786,432 bytes into native-ordered Direct ByteBuffer
        val lutSize = preset.lut.lutSize
        val lutExpectedBytes = lutSize * lutSize * lutSize * 3
        val lutBuffer = ByteBuffer.allocateDirect(lutExpectedBytes).order(ByteOrder.nativeOrder())

        assetManager.open(preset.lut.assetPath).use { inputStream ->
            readStreamToDirectBuffer(inputStream, lutBuffer, lutExpectedBytes)
        }
        lutBuffer.position(0)

        // 2. Load Light Leaks for this preset (if any)
        val lightLeaks = mutableMapOf<String, ByteArray>()
        for (leakPath in preset.compositing.lightLeakPool) {
            assetManager.open(leakPath).use { stream ->
                lightLeaks[leakPath] = stream.readBytes()
            }
        }

        // 3. Load Border Mask (if any)
        val borderBytes = preset.compositing.borderMaskAsset?.let { borderPath ->
            assetManager.open(borderPath).use { stream ->
                stream.readBytes()
            }
        }

        LoadedPresetAssets(
            preset = preset,
            lutDirectBuffer = lutBuffer,
            lightLeaks = lightLeaks,
            borderMaskBytes = borderBytes
        )
    }

    private fun readStreamToDirectBuffer(input: InputStream, buffer: ByteBuffer, totalBytes: Int) {
        val chunk = ByteArray(16384)
        var totalRead = 0
        while (totalRead < totalBytes) {
            val toRead = minOf(chunk.size, totalBytes - totalRead)
            val read = input.read(chunk, 0, toRead)
            if (read < 0) break
            buffer.put(chunk, 0, read)
            totalRead += read
        }
    }
}
