package com.vintagecam.domain.presets.repository

import android.content.Context
import com.vintagecam.domain.presets.model.CameraPreset
import com.vintagecam.domain.presets.model.PresetCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Repository interface providing access to the catalog of vintage camera and film stock presets.
 */
interface PresetRepository {
    /**
     * Retrieves all available camera presets in the catalog.
     */
    suspend fun getAllPresets(): List<CameraPreset>

    /**
     * Retrieves presets filtered by optical sensor or film stock category.
     */
    suspend fun getPresetsByCategory(category: PresetCategory): List<CameraPreset>

    /**
     * Finds a single preset by its unique identifier.
     * Returns null if no preset matches the given ID.
     */
    suspend fun getPresetById(id: String): CameraPreset?
}

/**
 * Thread-safe implementation of [PresetRepository] backed by assets/presets/presets_catalog.json.
 * Uses lazy in-memory caching protected by a coroutines [Mutex] to eliminate duplicate asset I/O.
 */
class PresetRepositoryImpl(
    private val context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) : PresetRepository {

    private val mutex = Mutex()
    private var cachedPresets: List<CameraPreset>? = null
    private var presetMapById: Map<String, CameraPreset> = emptyMap()
    private var presetMapByCategory: Map<PresetCategory, List<CameraPreset>> = emptyMap()

    private suspend fun ensureLoaded(): List<CameraPreset> {
        cachedPresets?.let { return it }

        return mutex.withLock {
            cachedPresets?.let { return it }

            withContext(Dispatchers.IO) {
                val jsonString = context.assets.open(CATALOG_ASSET_PATH).bufferedReader().use { it.readText() }
                val presets = json.decodeFromString<List<CameraPreset>>(jsonString)

                cachedPresets = presets
                presetMapById = presets.associateBy { it.id }
                presetMapByCategory = presets.groupBy { it.category }
                presets
            }
        }
    }

    override suspend fun getAllPresets(): List<CameraPreset> {
        return ensureLoaded()
    }

    override suspend fun getPresetsByCategory(category: PresetCategory): List<CameraPreset> {
        ensureLoaded()
        return presetMapByCategory[category] ?: emptyList()
    }

    override suspend fun getPresetById(id: String): CameraPreset? {
        ensureLoaded()
        return presetMapById[id]
    }

    companion object {
        const val CATALOG_ASSET_PATH = "presets/presets_catalog.json"
    }
}
