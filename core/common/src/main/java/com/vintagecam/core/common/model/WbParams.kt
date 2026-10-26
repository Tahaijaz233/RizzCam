package com.vintagecam.core.common.model

/**
 * Mutable, allocation-free container for volatile GPU uniform updates.
 * Thread-safe for multi-threaded render loop consumption.
 */
data class WbParams(
    @Volatile var temperature: Float = 6500f,
    @Volatile var tint: Float = 0f,
    @Volatile var strength: Float = 0f
)
