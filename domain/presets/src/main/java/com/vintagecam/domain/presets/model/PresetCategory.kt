package com.vintagecam.domain.presets.model

import kotlinx.serialization.Serializable

/**
 * Categorizes camera presets according to optical sensor and analog film medium taxonomy.
 * Matches Section 7 of the Clean-Room Architectural Specification:
 * - DIGITAL: Sharp CMOS/CCD compacts, harsh highlight roll-off, early 2000s digicams.
 * - VINTAGE_135: 35mm cartridge film stocks (e.g., Kodak Portra, Fuji Superia, CineStill 800T).
 * - VINTAGE_120: Medium format square and 6x7 film stocks (e.g., Holga 120, Pentax 6x7, Hasselblad).
 * - INSTANT: Peel-apart and integral self-developing chemistry (e.g., Polaroid SX-70, Instax).
 * - RETRO_VIDEO: Analog magnetic tape and early digital video camcorders (e.g., VHS, Super 8, MiniDV).
 */
@Serializable
enum class PresetCategory(
    val id: String,
    val displayName: String,
    val description: String
) {
    DIGITAL(
        id = "digital",
        displayName = "Digital",
        description = "Early CCD sensors, sharp digicam flash, and vintage digital aesthetics"
    ),
    VINTAGE_135(
        id = "vintage_135",
        displayName = "135 Film",
        description = "Classic 35mm cartridge negative, reversal, and cinema film stocks"
    ),
    VINTAGE_120(
        id = "vintage_120",
        displayName = "120 Medium",
        description = "Medium format 6x6, 6x7, and toy lens camera characteristics"
    ),
    INSTANT(
        id = "instant",
        displayName = "Instant",
        description = "Integral film dye diffusion, iconic white border frames, and chemical fades"
    ),
    RETRO_VIDEO(
        id = "retro_video",
        displayName = "Retro Video",
        description = "Magnetic tape scanlines, Super 8 grain, and retro camcorder colorimetry"
    )
}
