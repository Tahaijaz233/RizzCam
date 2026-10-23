package com.vintagecam.feature.viewfinder.ui.gl

/**
 * Redirection to the canonical [com.vintagecam.core.gl.egl.GlThreadExecutor].
 *
 * Prevents class duplication between `feature:viewfinder` and `core:gl-pipeline`.
 */
typealias GlThreadExecutor = com.vintagecam.core.gl.egl.GlThreadExecutor
