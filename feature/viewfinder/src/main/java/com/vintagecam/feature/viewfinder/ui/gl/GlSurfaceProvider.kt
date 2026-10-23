package com.vintagecam.feature.viewfinder.ui.gl

/**
 * Redirection to the canonical [com.vintagecam.core.camera.GlSurfaceProvider].
 *
 * Prevents class duplication between `feature:viewfinder` and `core:camera`.
 */
typealias GlSurfaceProvider = com.vintagecam.core.camera.GlSurfaceProvider
