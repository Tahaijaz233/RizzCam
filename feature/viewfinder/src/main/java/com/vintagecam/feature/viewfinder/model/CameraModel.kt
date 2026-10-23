package com.vintagecam.feature.viewfinder.model

import com.vintagecam.domain.presets.model.AspectRatioProfile

/**
 * Visual badge types displayed on camera card badges in the Dazz/RizzCam catalog.
 */
enum class CameraBadgeType {
    NONE,
    BETA,    // Cyan circular badge with white 'β'
    BLUE_R,  // Blue 'R' text indicator on model name
    RED_R,   // Red 'R' text indicator on model name
    PLAY     // Red circular badge with white play triangle '▶'
}

/**
 * Optical accessory filters attachable to vintage cameras.
 */
enum class OpticalAccessory(
    val id: String,
    val displayName: String,
    val description: String
) {
    NONE("none", "Clear", "Clear optical glass without filtration"),
    FISHEYE_F("fisheye_f", "Fisheye F", "Full-frame curvilinear fisheye conversion"),
    FISHEYE_W("fisheye_w", "Fisheye W", "Circular wide-angle fisheye lens"),
    ND_FILTER("nd_filter", "ND Filter", "Neutral density dark glass light attenuation"),
    FLASH_C("flash_c", "Flash C", "Warm amber/red theatrical flash gel filter"),
    PRISM("prism", "Prism", "Multi-faceted kaleidoscope glass refraction"),
    STAR("star", "Star", "6-point cross starburst highlight flare")
}

/**
 * Camera categories matching the reference designs.
 */
enum class DazzCategory(val displayName: String) {
    DIGITAL("DIGITAL"),
    VIDEO("VIDEO"),
    VINTAGE_135("VINTAGE 135"),
    VINTAGE_120("VINTAGE 120"),
    INST_COLLECTION("INST COLLECTION"),
    ACCESSORY("ACCESSORY"),
    LEGACY("LEGACY CAMERAS")
}

/**
 * Visual graphic rendering category for miniature vintage camera illustrations.
 */
enum class CameraGraphicType {
    RANGEFINDER_SILVER,
    RANGEFINDER_BLACK,
    COMPACT_TITANIUM,
    COMPACT_CHAMPAGNE,
    COMPACT_BLUE,
    DIGICAM_CCD,
    DIGICAM_BLACK,
    SMARTPHONE_PHOTO,
    SMARTPHONE_VIDEO,
    COLLAGE_FRAME,
    STEREO_3D,
    FILM_NEGATIVE_STRIP,
    TOY_CAMERA_YELLOW,
    TOY_CAMERA_RED,
    INFRARED_RED_BLUE,
    SLR_CLASSIC_SILVER,
    SLR_CLASSIC_BLACK,
    DISPOSABLE_GREEN,
    DISPOSABLE_NEON,
    MULTI_LENS_GOLF,
    HALF_FRAME_PEN,
    SLIDE_FRAME,
    TLR_ROLLEI,
    HOLGA_MEDIUM,
    PENTAX_67,
    HASSELBLAD_CUBE,
    INSTAX_WHITE,
    INSTAX_SQUARE_DARK,
    INSTAX_SQUARE_ORANGE,
    POLAROID_BELLOWS,
    CINE_16MM,
    SUPER_8_YELLOW,
    VHS_CAMCORDER,
    KINO_WHITE,
    VIDEO_FUN_YELLOW,
    HANDYCAM_GOLD,
    CYBER_GLOW_PINK,
    PROJECTOR_SLIDE,
    ACTION_CAM_DAM,
    TEAL_VINTAGE,
    SPY_CAM_SILVER,
    PHOTO_BOOTH,
    SPORTS_YELLOW
}

/**
 * Complete model representing a vintage camera body in the catalog.
 */
data class CameraModel(
    val id: String,
    val name: String,
    val category: DazzCategory,
    val isVideo: Boolean = false,
    val badge: CameraBadgeType = CameraBadgeType.NONE,
    val defaultFocalLengthMm: Int = 35,
    val aspectRatio: AspectRatioProfile = AspectRatioProfile.RATIO_4_3,
    val underlyingPresetId: String,
    val graphicType: CameraGraphicType,
    val description: String = "",
    val sampleTip: String = ""
)

/**
 * Authentic metadata and sample specifications for camera models.
 */
data class CameraSampleInfo(
    val cameraName: String,
    val subtitle: String,
    val sensorType: String,
    val iso: String,
    val dynamicRange: String,
    val grainProfile: String,
    val halationCharacteristics: String,
    val bestLighting: String,
    val sampleCaption: String
)

/**
 * Complete catalog containing all 45+ cameras and accessories from the reference designs.
 */
object DazzCameraCatalog {

    val allCameras: List<CameraModel> = listOf(
        // =====================================================================
        // DIGITAL
        // =====================================================================
        CameraModel(
            id = "original",
            name = "Original",
            category = DazzCategory.DIGITAL,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 28,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "ccd_power_shot",
            graphicType = CameraGraphicType.SMARTPHONE_PHOTO,
            description = "Natural digital mobile sensor rendering with crisp micro-contrast.",
            sampleTip = "Ideal for everyday snapshots and clean daylight documentary work."
        ),
        CameraModel(
            id = "grd_r",
            name = "GRD R",
            category = DazzCategory.DIGITAL,
            isVideo = false,
            badge = CameraBadgeType.BLUE_R,
            defaultFocalLengthMm = 28,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "ccd_power_shot",
            graphicType = CameraGraphicType.DIGICAM_BLACK,
            description = "High-contrast snap focus street legend with punchy monochrome and vivid reds.",
            sampleTip = "Master of fast hip-shots and gritty urban street scenes."
        ),
        CameraModel(
            id = "ccd_r",
            name = "CCD R",
            category = DazzCategory.DIGITAL,
            isVideo = false,
            badge = CameraBadgeType.BLUE_R,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "ccd_power_shot",
            graphicType = CameraGraphicType.DIGICAM_CCD,
            description = "Early 2000s CCD color science with direct strobe flash and creamy skin tones.",
            sampleTip = "Unbeatable for late night party flash portraits."
        ),
        CameraModel(
            id = "collage",
            name = "Collage",
            category = DazzCategory.DIGITAL,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_1_1,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.COLLAGE_FRAME,
            description = "Multi-shot sequential diptych framing with vintage tonal harmony.",
            sampleTip = "Tells a story in two connected sequential moments."
        ),
        CameraModel(
            id = "fxn_r",
            name = "FXN R",
            category = DazzCategory.DIGITAL,
            isVideo = false,
            badge = CameraBadgeType.BLUE_R,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.RANGEFINDER_SILVER,
            description = "Film simulation hybrid mimicking high-end Japanese rangefinder optics.",
            sampleTip = "Excels at environmental portraits and golden hour light."
        ),

        // =====================================================================
        // VIDEO
        // =====================================================================
        CameraModel(
            id = "v_classic",
            name = "V Classic",
            category = DazzCategory.VIDEO,
            isVideo = true,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "cinestill_800t",
            graphicType = CameraGraphicType.TEAL_VINTAGE,
            description = "Classic broadcast analog video with warm tungsten glow.",
            sampleTip = "Cinematic video with soft highlight roll-off."
        ),
        CameraModel(
            id = "original_v",
            name = "Original V",
            category = DazzCategory.VIDEO,
            isVideo = true,
            badge = CameraBadgeType.PLAY,
            defaultFocalLengthMm = 28,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "cinestill_800t",
            graphicType = CameraGraphicType.SMARTPHONE_VIDEO,
            description = "Zero-latency mobile video recording with rich natural color.",
            sampleTip = "Perfect for high-definition retro vlogs and memories."
        ),
        CameraModel(
            id = "dam",
            name = "DAM",
            category = DazzCategory.VIDEO,
            isVideo = true,
            badge = CameraBadgeType.BETA,
            defaultFocalLengthMm = 24,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "cinestill_800t",
            graphicType = CameraGraphicType.ACTION_CAM_DAM,
            description = "Wide-angle action perspective with punchy dynamic saturation.",
            sampleTip = "Action sports and fast handheld movement."
        ),
        CameraModel(
            id = "16mm",
            name = "16mm",
            category = DazzCategory.VIDEO,
            isVideo = true,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 28,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "cinestill_800t",
            graphicType = CameraGraphicType.CINE_16MM,
            description = "Bolex 16mm motion picture film stock with organic grain weave.",
            sampleTip = "Authentic indie film aesthetic with gentle gate jitter."
        ),
        CameraModel(
            id = "8mm",
            name = "8mm",
            category = DazzCategory.VIDEO,
            isVideo = true,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "cinestill_800t",
            graphicType = CameraGraphicType.SUPER_8_YELLOW,
            description = "Super 8 cartridge home movie film with nostalgic warm grain and halation.",
            sampleTip = "Sun-drenched summer memories and road trips."
        ),
        CameraModel(
            id = "vhs",
            name = "VHS",
            category = DazzCategory.VIDEO,
            isVideo = true,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "cinestill_800t",
            graphicType = CameraGraphicType.VHS_CAMCORDER,
            description = "Magnetic tape scanlines, chromatic bleeding, and date-stamp fidelity.",
            sampleTip = "1990s retro home video aesthetic."
        ),
        CameraModel(
            id = "kino",
            name = "Kino",
            category = DazzCategory.VIDEO,
            isVideo = true,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "cinestill_800t",
            graphicType = CameraGraphicType.KINO_WHITE,
            description = "European cinema reel simulation with delicate contrast curves.",
            sampleTip = "Thoughtful arthouse cinematography."
        ),
        CameraModel(
            id = "v_funs",
            name = "V FunS",
            category = DazzCategory.VIDEO,
            isVideo = true,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "cinestill_800t",
            graphicType = CameraGraphicType.VIDEO_FUN_YELLOW,
            description = "Playful toy video camera with saturated pop-color response.",
            sampleTip = "Vibrant casual party and festival footage."
        ),
        CameraModel(
            id = "dcr",
            name = "DCR",
            category = DazzCategory.VIDEO,
            isVideo = true,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "ccd_power_shot",
            graphicType = CameraGraphicType.HANDYCAM_GOLD,
            description = "Early MiniDV digital camcorder with sharp nostalgic edge definition.",
            sampleTip = "Millennium era music videos and skate clips."
        ),
        CameraModel(
            id = "glow",
            name = "Glow",
            category = DazzCategory.VIDEO,
            isVideo = true,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "cinestill_800t",
            graphicType = CameraGraphicType.CYBER_GLOW_PINK,
            description = "Ethereal dream bloom with soft pastel highlights.",
            sampleTip = "Dreamy night city lights and fashion clips."
        ),
        CameraModel(
            id = "slide_p",
            name = "Slide P",
            category = DazzCategory.VIDEO,
            isVideo = true,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.PROJECTOR_SLIDE,
            description = "Slide projection illumination with optical slide frame gate.",
            sampleTip = "Vintage projector slideshow presentation."
        ),

        // =====================================================================
        // VINTAGE 135
        // =====================================================================
        CameraModel(
            id = "d_classic",
            name = "D Classic",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.RANGEFINDER_SILVER,
            description = "Signature 35mm rangefinder with authentic color negative film emulsion.",
            sampleTip = "The everyday master of skin tones and street candids."
        ),
        CameraModel(
            id = "gr_f",
            name = "GR F",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 28,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.RANGEFINDER_BLACK,
            description = "Compact 28mm wide prime lens with needle-sharp edge rendering.",
            sampleTip = "Broad landscapes and urban architecture."
        ),
        CameraModel(
            id = "ct2f",
            name = "CT2F",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 38,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "contax_t2_party",
            graphicType = CameraGraphicType.COMPACT_TITANIUM,
            description = "Luxury titanium compact with Sonnar f/2.8 lens and brilliant flash falloff.",
            sampleTip = "Flash party snapshots with unmistakable celebrity glamour."
        ),
        CameraModel(
            id = "d3d",
            name = "D3D",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 30,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.STEREO_3D,
            description = "Triple-lens stereo camera capturing parallax wiggle depth.",
            sampleTip = "Portraits with dynamic background separation."
        ),
        CameraModel(
            id = "135_ne",
            name = "135 NE",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.FILM_NEGATIVE_STRIP,
            description = "Exposed 35mm negative film border including edge sprockets and frame numbers.",
            sampleTip = "Gives shots an undeniable authentic darkroom proof-sheet vibe."
        ),
        CameraModel(
            id = "d_funs",
            name = "D FunS",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 32,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.TOY_CAMERA_YELLOW,
            description = "Vibrant disposable camera with optical meniscus lens distortion.",
            sampleTip = "Beach trips and sunlit daytime adventures."
        ),
        CameraModel(
            id = "ir",
            name = "IR",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "aerochrome_ir",
            graphicType = CameraGraphicType.INFRARED_RED_BLUE,
            description = "False-color Kodak Aerochrome infrared turning chlorophyll foliage electric magenta.",
            sampleTip = "Sunlit foliage, landscapes, and surreal nature walks."
        ),
        CameraModel(
            id = "classic_u",
            name = "Classic U",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 50,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.SLR_CLASSIC_SILVER,
            description = "Mechanical SLR with 50mm f/1.4 prime lens and rich bokeh transitions.",
            sampleTip = "Intimate portraiture with buttery shallow depth of field."
        ),
        CameraModel(
            id = "dqs",
            name = "DQS",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 32,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "contax_t2_party",
            graphicType = CameraGraphicType.DISPOSABLE_GREEN,
            description = "Single-use QuickSnap disposable film camera with high-speed 400 film.",
            sampleTip = "Nostalgic everyday candids and disposable flash aesthetics."
        ),
        CameraModel(
            id = "fqs_r",
            name = "FQS R",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.BLUE_R,
            defaultFocalLengthMm = 32,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "fuji_superia_half",
            graphicType = CameraGraphicType.DISPOSABLE_NEON,
            description = "Japanese disposable film simulation with emerald greens and cool shadows.",
            sampleTip = "Great in twilight and neon cityscapes."
        ),
        CameraModel(
            id = "golf",
            name = "Golf",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.RED_R,
            defaultFocalLengthMm = 28,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.MULTI_LENS_GOLF,
            description = "8-lens sequential action shutter capturing high-speed continuous motion.",
            sampleTip = "Sports swings, dance moves, and skateboard tricks."
        ),
        CameraModel(
            id = "cpm35",
            name = "CPM35",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "contax_t2_party",
            graphicType = CameraGraphicType.COMPACT_CHAMPAGNE,
            description = "Iconic clamshell 35mm compact (Mju-II styling) with pinpoint autofocus flash.",
            sampleTip = "Pocketable night street and indoor party portraits."
        ),
        CameraModel(
            id = "srx_r",
            name = "SRX R",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.BLUE_R,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.COMPACT_BLUE,
            description = "Vibrant retro point-and-shoot with deep saturation and cool highlights.",
            sampleTip = "Bright beach days and outdoor summer events."
        ),
        CameraModel(
            id = "d_half",
            name = "D Half",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 30,
            aspectRatio = AspectRatioProfile.HALF_FRAME_DIPTYCH,
            underlyingPresetId = "fuji_superia_half",
            graphicType = CameraGraphicType.HALF_FRAME_PEN,
            description = "Half-frame 35mm capturing 72 exposures; auto stitches 2 vertical frames into 1.",
            sampleTip = "Pair an establishing wide shot with a detail close-up."
        ),
        CameraModel(
            id = "d_slide",
            name = "D Slide",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.SLIDE_FRAME,
            description = "Color positive reversal slide film mounted inside an authentic paper slide mount.",
            sampleTip = "Deep contrast and velvety dark blacks."
        ),
        CameraModel(
            id = "ofm_r",
            name = "OFM R",
            category = DazzCategory.VINTAGE_135,
            isVideo = false,
            badge = CameraBadgeType.BETA,
            defaultFocalLengthMm = 50,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.SLR_CLASSIC_BLACK,
            description = "Manual mechanical SLR body with precision mechanical shutter simulation.",
            sampleTip = "Artistic portraits and fine art still life."
        ),

        // =====================================================================
        // VINTAGE 120
        // =====================================================================
        CameraModel(
            id = "s_classic",
            name = "S Classic",
            category = DazzCategory.VINTAGE_120,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 50,
            aspectRatio = AspectRatioProfile.RATIO_1_1,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.TLR_ROLLEI,
            description = "Twin Lens Reflex 6x6 square medium format with waist-level perspective.",
            sampleTip = "Square compositions with elegant vertical posture."
        ),
        CameraModel(
            id = "hoga",
            name = "HOGA",
            category = DazzCategory.VINTAGE_120,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 45,
            aspectRatio = AspectRatioProfile.RATIO_1_1,
            underlyingPresetId = "holga_120n",
            graphicType = CameraGraphicType.HOLGA_MEDIUM,
            description = "Cult 120 toy camera featuring heavy vignetting, light leaks, and soft edge blur.",
            sampleTip = "Experimental dreamscapes and moody square portraits."
        ),
        CameraModel(
            id = "s_67",
            name = "S 67",
            category = DazzCategory.VINTAGE_120,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 90,
            aspectRatio = AspectRatioProfile.RATIO_6_7,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.PENTAX_67,
            description = "The king of 6x7 medium format: magnificent 105mm f/2.4 aesthetic depth.",
            sampleTip = "Editorial fashion portraits with razor-sharp subject isolation."
        ),
        CameraModel(
            id = "kv80_r",
            name = "KV80 R",
            category = DazzCategory.VINTAGE_120,
            isVideo = false,
            badge = CameraBadgeType.BLUE_R,
            defaultFocalLengthMm = 80,
            aspectRatio = AspectRatioProfile.RATIO_1_1,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.HASSELBLAD_CUBE,
            description = "Modular cube medium format camera with Zeiss Planar 80mm color fidelity.",
            sampleTip = "Studio portraits and commercial still-life compositions."
        ),

        // =====================================================================
        // INST COLLECTION
        // =====================================================================
        CameraModel(
            id = "inst_c",
            name = "Inst C",
            category = DazzCategory.INST_COLLECTION,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "polaroid_sx70",
            graphicType = CameraGraphicType.INSTAX_WHITE,
            description = "Contemporary mini instant camera with glowing highlights and chemical fade.",
            sampleTip = "Warm friends hangouts and instant memories."
        ),
        CameraModel(
            id = "inst_sq",
            name = "Inst SQ",
            category = DazzCategory.INST_COLLECTION,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_1_1,
            underlyingPresetId = "polaroid_sx70",
            graphicType = CameraGraphicType.INSTAX_SQUARE_DARK,
            description = "Square instant format with rich contrast and sharp integral development.",
            sampleTip = "Balanced centered portraits and symmetry."
        ),
        CameraModel(
            id = "inst_sqc",
            name = "Inst SQC",
            category = DazzCategory.INST_COLLECTION,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_1_1,
            underlyingPresetId = "polaroid_sx70",
            graphicType = CameraGraphicType.INSTAX_SQUARE_ORANGE,
            description = "Color-filtered instant square with warm retro hue shifting.",
            sampleTip = "Cozy indoor scenes and sunset light."
        ),
        CameraModel(
            id = "paf_r",
            name = "PAF R",
            category = DazzCategory.INST_COLLECTION,
            isVideo = false,
            badge = CameraBadgeType.BETA,
            defaultFocalLengthMm = 50,
            aspectRatio = AspectRatioProfile.RATIO_1_1,
            underlyingPresetId = "polaroid_sx70",
            graphicType = CameraGraphicType.POLAROID_BELLOWS,
            description = "Folding SLR SX-70 instant camera with glass 116mm f/8 lens.",
            sampleTip = "Fine-art closeups and artistic Polaroid portraiture."
        ),

        // =====================================================================
        // LEGACY CAMERAS
        // =====================================================================
        CameraModel(
            id = "d_exp",
            name = "D Exp",
            category = DazzCategory.LEGACY,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.TEAL_VINTAGE,
            description = "Experimental early film profile with high contrast grain.",
            sampleTip = "Moody vintage snapshots."
        ),
        CameraModel(
            id = "nt16",
            name = "NT16",
            category = DazzCategory.LEGACY,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 28,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.SPY_CAM_SILVER,
            description = "Miniature spy camera with micro-lens characteristics.",
            sampleTip = "Candid pocket shots."
        ),
        CameraModel(
            id = "inst_ss",
            name = "Inst SS",
            category = DazzCategory.LEGACY,
            isVideo = true,
            badge = CameraBadgeType.PLAY,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "polaroid_sx70",
            graphicType = CameraGraphicType.COLLAGE_FRAME,
            description = "Instant motion stop-motion video simulation.",
            sampleTip = "Fun stop-motion loops."
        ),
        CameraModel(
            id = "puli",
            name = "Puli",
            category = DazzCategory.LEGACY,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_4_3,
            underlyingPresetId = "ccd_power_shot",
            graphicType = CameraGraphicType.PHOTO_BOOTH,
            description = "Retro photobooth 4-strip camera simulation.",
            sampleTip = "Fun duo or couple booth photos."
        ),
        CameraModel(
            id = "kv88",
            name = "KV88",
            category = DazzCategory.LEGACY,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 80,
            aspectRatio = AspectRatioProfile.RATIO_1_1,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.HASSELBLAD_CUBE,
            description = "Classic medium format studio simulation with early color curve.",
            sampleTip = "Square format portraiture."
        ),
        CameraModel(
            id = "135_sr",
            name = "135 SR",
            category = DazzCategory.LEGACY,
            isVideo = false,
            badge = CameraBadgeType.NONE,
            defaultFocalLengthMm = 35,
            aspectRatio = AspectRatioProfile.RATIO_3_2,
            underlyingPresetId = "portra_400_35mm",
            graphicType = CameraGraphicType.SPORTS_YELLOW,
            description = "Weather-sealed action camera simulation with vibrant primary colors.",
            sampleTip = "Outdoor adventure snapshots."
        )
    )

    val photoCameras: List<CameraModel> get() = allCameras.filter { !it.isVideo && it.category != DazzCategory.LEGACY }
    val videoCameras: List<CameraModel> get() = allCameras.filter { it.isVideo && it.category != DazzCategory.LEGACY }
    val legacyCameras: List<CameraModel> get() = allCameras.filter { it.category == DazzCategory.LEGACY }

    val camerasByCategory: Map<DazzCategory, List<CameraModel>> get() = allCameras.groupBy { it.category }

    fun findById(id: String): CameraModel =
        allCameras.firstOrNull { it.id == id } ?: allCameras.first()

    fun getSampleInfo(camera: CameraModel): CameraSampleInfo = CameraSampleInfo(
        cameraName = camera.name,
        subtitle = camera.description,
        sensorType = if (camera.isVideo) "Analog Video / Motion Simulation" else "${camera.category.displayName} Film Simulation",
        iso = when (camera.category) {
            DazzCategory.DIGITAL -> "ISO 100 - 400 (CCD Native)"
            DazzCategory.VIDEO -> "ISO 500T (Cinema Tungsten)"
            DazzCategory.VINTAGE_135 -> "ISO 400 (C-41 Negative)"
            DazzCategory.VINTAGE_120 -> "ISO 160 (Medium Format)"
            DazzCategory.INST_COLLECTION -> "ISO 640 (Integral Chemistry)"
            DazzCategory.LEGACY -> "ISO 200 (Vintage Stock)"
            else -> "ISO 400"
        },
        dynamicRange = when (camera.category) {
            DazzCategory.DIGITAL -> "10.2 Stops (Sharp Digicam Clip)"
            DazzCategory.VIDEO -> "12.5 Stops (Soft Highlight Bloom)"
            DazzCategory.VINTAGE_135 -> "14.0 Stops (High Latitude)"
            DazzCategory.VINTAGE_120 -> "15.0 Stops (Extreme Latitude)"
            DazzCategory.INST_COLLECTION -> "9.0 Stops (Punchy Chemistry)"
            else -> "12.0 Stops"
        },
        grainProfile = when (camera.category) {
            DazzCategory.DIGITAL -> "Mild sensor noise with chromatic speckles"
            DazzCategory.VIDEO -> "Organic motion film weave & video jitter"
            DazzCategory.VINTAGE_135 -> "T-Grain fine silver halide distribution"
            DazzCategory.VINTAGE_120 -> "Ultra-fine medium format resolution"
            DazzCategory.INST_COLLECTION -> "Chemical dye diffusion texture"
            else -> "Organic analog grain"
        },
        halationCharacteristics = when (camera.category) {
            DazzCategory.VIDEO -> "Red tungsten halation around direct light sources"
            DazzCategory.VINTAGE_135 -> "Subtle orange rem-jet anti-halation glow"
            DazzCategory.INST_COLLECTION -> "Soft highlight diffusion bloom"
            else -> "Natural optical highlight roll-off"
        },
        bestLighting = camera.sampleTip,
        sampleCaption = "Shot on ${camera.name} with ${camera.defaultFocalLengthMm}mm vintage optical simulation"
    )
}
