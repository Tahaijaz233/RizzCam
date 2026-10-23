package com.vintagecam.core.gl.fbo

/**
 * Ping-pong framebuffer allocator managing dual full-resolution FBOs and dual quarter-resolution FBOs.
 *
 * Architecture & Memory Budget:
 * - Preview Mode (1920x1080):
 *     fboA (RGBA16F): ~16.6 MB
 *     fboB (RGBA16F): ~16.6 MB
 *     quarterFbo1 (480x270 RGBA16F): ~1.04 MB
 *     quarterFbo2 (480x270 RGBA16F): ~1.04 MB
 *     Total VRAM footprint: ~35.3 MB (strictly below the 250 MB preview ceiling)
 *
 * - Still Capture Mode (4000x3000 peak):
 *     fboA (RGBA16F): ~96 MB
 *     fboB (RGBA16F): ~96 MB
 *     quarterFbo1 (1000x750 RGBA16F): ~6 MB
 *     quarterFbo2 (1000x750 RGBA16F): ~6 MB
 *     Total VRAM footprint: ~204 MB (strictly below the 600 MB capture ceiling)
 *
 * Implements deterministic [release] to free all textures and FBO handles simultaneously.
 */
class PingPongFboPool(
    val width: Int,
    val height: Int,
    useFloat16: Boolean = true
) {
    val fboA = FramebufferObject(width, height, useFloat16 = useFloat16)
    val fboB = FramebufferObject(width, height, useFloat16 = useFloat16)

    // Quarter-resolution FBOs dedicated to Pass 3 (Halation threshold + separable Gaussian blur)
    val quarterFbo1 = FramebufferObject(
        (width / 4).coerceAtLeast(1),
        (height / 4).coerceAtLeast(1),
        useFloat16 = useFloat16
    )
    val quarterFbo2 = FramebufferObject(
        (width / 4).coerceAtLeast(1),
        (height / 4).coerceAtLeast(1),
        useFloat16 = useFloat16
    )

    private var isAWriteTarget = true

    /**
     * The FBO currently holding the output of the previous render pass.
     */
    val readFbo: FramebufferObject
        get() = if (isAWriteTarget) fboB else fboA

    /**
     * The FBO currently bound as the destination for the active render pass.
     */
    val writeFbo: FramebufferObject
        get() = if (isAWriteTarget) fboA else fboB

    /**
     * Swaps the read and write buffers after a pass completes.
     */
    fun swap() {
        isAWriteTarget = !isAWriteTarget
    }

    /**
     * Resets the ping-pong state so fboA is the initial write target.
     */
    fun reset() {
        isAWriteTarget = true
    }

    /**
     * Deterministically releases all 4 underlying FramebufferObjects.
     */
    fun release() {
        fboA.release()
        fboB.release()
        quarterFbo1.release()
        quarterFbo2.release()
    }
}
