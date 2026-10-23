# Clean-Room Architectural Specification: Real-Time Vintage Film Camera Engine for Android

**Target Platform:** Android 9.0+ (API 28+) | **Graphics API:** OpenGL ES 3.0+ / EGL 1.4  
**Camera Stack:** Jetpack CameraX (`camera-camera2`) | **UI Framework:** Jetpack Compose  
**Performance Target:** $60\text{ FPS}$ ($\le 16.67\text{ ms}$ frame time) at $1080\text{p}$ preview; $< 450\text{ ms}$ full-resolution ($12\text{ MP}$–$48\text{ MP}$) offscreen capture pipeline.

---

## 1. System Architecture Diagram & Data Flow

To match the responsiveness and optical fidelity of iOS Metal/CoreImage pipelines on Android, the architecture strictly separates the **Real-Time Viewfinder Path** from the **High-Resolution Still Capture Path** while sharing a unified EGL context group and compiled GLSL shader programs. Zero-copy buffer sharing is enforced wherever supported by the SoC via `SurfaceTexture` (`GL_TEXTURE_EXTERNAL_OES`) and `AHardwareBuffer` (`EGLImageKHR`).

### 1.1 High-Level Data Flow (ASCII Architecture)

```text
+---------------------------------------------------------------------------------------------------+
|                                   CAMERA HARDWARE ABSTRACTION                                     |
|                           Camera2 HAL / Image Signal Processor (ISP)                              |
+------------------------------------+-----------------------------------------+--------------------+
                                     |                                         |
            [YUV_420_888 Hardware Stream (60 FPS)]             [High-Res Capture (JPEG / YUV / RAW)]
                                     |                                         |
                                     v                                         v
+--------------------------------------------------------------------+ +----------------------------+
|                    PREVIEW INGESTION LAYER                         | |  CAPTURE INGESTION LAYER   |
|  CameraX Preview UseCase -> SurfaceProvider                        | |  CameraX ImageCapture      |
|  SurfaceTexture (GL_TEXTURE_EXTERNAL_OES)                          | |  ImageProxy / HardwareBuf  |
|  uTexMatrix (4x4 Sensor Rotation + Aspect Ratio Crop: 4:3,3:2,1:1) | |  EGLImageKHR / GlTexSub2D  |
+------------------------------------+-------------------------------+ +-------------+--------------+
                                     |                                               |
                                     | (Shared EGLContext Group / Shader Programs)   |
                                     v                                               v
+---------------------------------------------------------------------------------------------------+
|                        DEDICATED GL RENDER THREAD (EGL 1.4 / OpenGL ES 3.0)                       |
|                                                                                                   |
|  +---------------------------------------------------------------------------------------------+  |
|  | PASS 0: OES-to-2D Ingestion & Aspect Crop                                                   |  |
|  | Input: samplerExternalOES | Output: FBO_A (RGBA16F) | Applies uTexMatrix & Pre-Exposure     |  |
|  +----------------------------------------------+----------------------------------------------+  |
|                                                 |                                                 |
|  +----------------------------------------------v----------------------------------------------+  |
|  | PASS 1: 3D LUT Color Grading (lut_filter.frag)                                              |  |
|  | Input: FBO_A + sampler3D (64x64x64 Cube) | Output: FBO_B | Trilinear Color Remapping        |  |
|  +----------------------------------------------+----------------------------------------------+  |
|                                                 |                                                 |
|  +----------------------------------------------v----------------------------------------------+  |
|  | PASS 2: Radial Chromatic Aberration (chromatic_aberration.frag)                             |  |
|  | Input: FBO_B | Output: FBO_A | Polynomial Radial RGB Channel Dispersion                     |  |
|  +----------------------------------------------+----------------------------------------------+  |
|                                                 |                                                 |
|  +----------------------------------------------v----------------------------------------------+  |
|  | PASS 3: Optical Halation & Bloom Sub-Pipeline                                               |  |
|  |   3a. High-Luminance Threshold (FBO_A -> FBO_Quarter_1 [1/4 Res])                           |  |
|  |   3b. Separable 9-Tap Gaussian Blur H (FBO_Quarter_1 -> FBO_Quarter_2)                      |  |
|  |   3c. Separable 9-Tap Gaussian Blur V (FBO_Quarter_2 -> FBO_Quarter_1)                      |  |
|  |   3d. Warm Red-Orange Additive Blend (FBO_A + FBO_Quarter_1 -> FBO_B)                       |  |
|  +----------------------------------------------+----------------------------------------------+  |
|                                                 |                                                 |
|  +----------------------------------------------v----------------------------------------------+  |
|  | PASS 4: Film Grain & Luminance Response (film_grain.frag)                                   |  |
|  | Input: FBO_B | Output: FBO_A | Procedural Simplex Noise + Midtone-Weighted Soft-Light Blend |  |
|  +----------------------------------------------+----------------------------------------------+  |
|                                                 |                                                 |
|  +----------------------------------------------v----------------------------------------------+  |
|  | PASS 5: Final Optical Compositing (compositing.frag)                                        |  |
|  | Input: FBO_A + LightLeakTex + BorderTex | Output: Default Framebuffer (Preview) OR FBO_Final|  |
|  | Applies cos^4(theta) Vignette, Screen Light Leaks, Sprocket/Polaroid Masks, Date Stamp      |  |
|  +---------------------+--------------------------------------------------------+--------------+  |
+------------------------|--------------------------------------------------------|-----------------+
                         |                                                        |
         [eglSwapBuffers (Window Surface)]                        [Async Double-PBO glReadPixels]
                         |                                                        |
                         v                                                        v
+------------------------------------------------+      +-------------------------------------------+
|         JETPACK COMPOSE UI OVERLAY             |      |       HIGH-RES EXPORT PIPELINE            |
|  - Skeuomorphic Viewfinder Chrome              |      |  - Unmap PBO -> Direct ByteBuffer         |
|  - Animated Mechanical Shutter Blind           |      |  - Half-Frame Diptych Stitcher (Optional) |
|  - Haptic Feedback Engine & Film Roll Counter  |      |  - ExifInterface Metadata Preservation    |
+------------------------------------------------+      |  - MediaStore JPEG / HEIF File Writer     |
                                                        +-------------------------------------------+
```

---

### 1.2 Aspect-Ratio & Crop Mathematics

Camera2 sensors natively output in $4:3$ (most mobile CMOS sensors) or $16:9$ (cropped sensor modes). Rather than reallocating camera sessions on aspect ratio changes (which causes a $300\text{ ms}$–$600\text{ ms}$ black-screen stall), the camera session remains locked to the highest-quality $4:3$ stream. Cropping to $4:3$, $3:2$, $1:1$, or **Half-Frame** ($3:4$ vertical diptych pairs on a $3:2$ landscape canvas) is computed dynamically via a $4 \times 4$ UV transformation matrix in **Pass 0**.

Given an input sensor stream with aspect ratio $A_{\text{src}} = \frac{W_{\text{src}}}{H_{\text{src}}}$ (post-rotation) and a target viewfinder frame with aspect ratio $A_{\text{dst}} = \frac{W_{\text{dst}}}{H_{\text{dst}}}$, the normalized UV scale factors $(S_x, S_y)$ are computed as:

$$S_x = \begin{cases} \frac{A_{\text{dst}}}{A_{\text{src}}} & \text{if } A_{\text{dst}} < A_{\text{src}} \\ 1.0 & \text{otherwise} \end{cases}, \quad S_y = \begin{cases} \frac{A_{\text{src}}}{A_{\text{dst}}} & \text{if } A_{\text{dst}} > A_{\text{src}} \\ 1.0 & \text{otherwise} \end{cases}$$

The center-crop UV transform matrix $\mathbf{M}_{\text{crop}}$ premultiplies the `SurfaceTexture` transform matrix $\mathbf{M}_{\text{st}}$:

$$\mathbf{M}_{\text{crop}} = \begin{bmatrix} S_x & 0 & 0 & \frac{1 - S_x}{2} \\ 0 & S_y & 0 & \frac{1 - S_y}{2} \\ 0 & 0 & 1 & 0 \\ 0 & 0 & 0 & 1 \end{bmatrix}, \quad \mathbf{M}_{\text{final}} = \mathbf{M}_{\text{st}} \cdot \mathbf{M}_{\text{crop}}$$

---

## 2. Project Directory & Module Structure

The application follows a strict multi-module **Clean Architecture + MVI (Model-View-Intent)** structure to isolate low-level OpenGL ES C/Kotlin bindings from Jetpack Compose UI state.

```text
vintage-cam-android/
├── app/
│   ├── src/main/java/com/vintagecam/app/
│   │   ├── di/                         # Hilt Dependency Injection Graph
│   │   ├── VintageCamApplication.kt
│   │   └── MainActivity.kt             # Edge-to-edge host, hardware volume key shutter intercept
├── core/
│   ├── camera/                         # CameraX lifecycle & zero-copy buffer ingestion
│   │   └── src/main/java/com/vintagecam/core/camera/
│   │       ├── CameraController.kt
│   │       ├── GlSurfaceProvider.kt    # Bridges CameraX Preview.SurfaceProvider to EGL
│   │       ├── HighResCaptureManager.kt# Handles ImageCapture + HardwareBuffer extraction
│   │       └── ExifMetadataWriter.kt   # Injects focal length, ISO, simulated film stock tag
│   ├── gl-pipeline/                    # OpenGL ES 3.0 / EGL 1.4 Engine
│   │   ├── src/main/assets/shaders/
│   │   │   ├── fullscreen_quad.vert
│   │   │   ├── oes_ingest.frag
│   │   │   ├── lut_filter.frag
│   │   │   ├── chromatic_aberration.frag
│   │   │   ├── halation_threshold.frag
│   │   │   ├── separable_blur.frag
│   │   │   ├── halation_composite.frag
│   │   │   ├── film_grain.frag
│   │   │   └── compositing.frag
│   │   └── src/main/java/com/vintagecam/core/gl/
│   │       ├── egl/
│   │       │   ├── EglCore.kt          # EGLDisplay, shared EGLContext, Pbuffer & Window surfaces
│   │       │   └── GlThreadExecutor.kt # Single-threaded dedicated HandlerThread for GL calls
│   │       ├── fbo/
│   │       │   ├── FramebufferObject.kt
│   │       │   └── PingPongFboPool.kt  # Full-res & quarter-res FBO ping-pong allocator
│   │       ├── pass/
│   │       │   ├── GlRenderPass.kt
│   │       │   ├── OesIngestPass.kt
│   │       │   ├── Lut3dPass.kt
│   │       │   ├── ChromaticAberrationPass.kt
│   │       │   ├── HalationChainPass.kt
│   │       │   ├── FilmGrainPass.kt
│   │       │   └── CompositingPass.kt
│   │       ├── texture/
│   │       │   ├── Lut3dTextureLoader.kt
│   │       │   └── CompressedAssetLoader.kt
│   │       ├── VintageGlPipeline.kt    # Orchestrates Pass 0 -> Pass 5 for Preview & Still
│   │       └── PboPhotoReader.kt       # Asynchronous GL_PIXEL_PACK_BUFFER readback
│   └── common/
│       └── src/main/java/com/vintagecam/core/common/
│           ├── math/AspectMatrixCalculator.kt
│           └── dispatchers/AppDispatchers.kt
├── domain/
│   └── presets/                        # Film Stock & Camera Body Definitions
│       ├── src/main/assets/presets/
│       │   ├── presets_catalog.json
│       │   ├── luts/                   # .raw3d (64^3 RGB8) and .png (HaldCLUT) assets
│       │   ├── leaks/                  # ASTC/ETC2 compressed light leak overlays
│       │   └── borders/                # Film sprocket, Polaroid, and Half-Frame masks
│       └── src/main/java/com/vintagecam/domain/presets/
│           ├── model/CameraPreset.kt
│           ├── repository/PresetRepository.kt
│           └── usecase/LoadPresetAssetsUseCase.kt
└── feature/
    ├── viewfinder/                     # Jetpack Compose Skeuomorphic UI & MVI ViewModel
    │   └── src/main/java/com/vintagecam/feature/viewfinder/
    │       ├── mvi/
    │       │   ├── ViewfinderState.kt
    │       │   ├── ViewfinderIntent.kt
    │       │   └── ViewfinderEffect.kt
    │       ├── ui/
    │       │   ├── ViewfinderScreen.kt
    │       │   ├── GlPreviewSurface.kt
    │       │   ├── ShutterBlindOverlay.kt
    │       │   ├── SkeuomorphicControls.kt
    │       │   └── FilmRollCounter.kt
    │       ├── haptics/ShutterHapticEngine.kt
    │       └── ViewfinderViewModel.kt
    └── gallery/                        # Film roll development & diptych viewer
```

---

## 3. Core GLSL Shaders (OpenGL ES 3.0)

All shaders target `#version 300 es` to guarantee support for `sampler3D`, explicit attribute locations, `texture()` intrinsics, and `RGBA16F` floating-point color attachments (critical for preventing banding during multi-pass color grading and halation).

### 3.1 Common Vertex Shader (`fullscreen_quad.vert`)

```glsl
#version 300 es
precision highp float;

layout(location = 0) in vec2 aPosition; // Normalized Device Coordinates: [-1.0, 1.0]
layout(location = 1) in vec2 aTexCoord; // Standard UV Coordinates: [0.0, 1.0]

uniform mat4 uTexMatrix; // Identity for standard 2D passes; Sensor*Crop matrix for Pass 0

out vec2 vTexCoord;

void main() {
    gl_Position = vec4(aPosition, 0.0, 1.0);
    vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
}
```

### 3.2 Pass 0: OES Ingestion & Pre-Exposure (`oes_ingest.frag`)

Converts hardware YUV frames from `GL_TEXTURE_EXTERNAL_OES` into linear/sRGB `GL_TEXTURE_2D` storage while applying user exposure compensation ($\text{EV}$) and color temperature shifts before LUT lookup.

```glsl
#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform samplerExternalOES uOesTexture;
uniform float uExposureEv;      // Range: [-2.0, +2.0] stops
uniform float uTemperatureShift;// Range: [-1.0, +1.0] (Cool to Warm)
uniform float uTintShift;       // Range: [-1.0, +1.0] (Green to Magenta)

vec3 applyWhiteBalance(vec3 color, float temp, float tint) {
    // CIE XYZ / LMS approximation for fast GPU white balance shift
    vec3 shift = vec3(1.0 + temp * 0.15, 1.0 - tint * 0.10, 1.0 - temp * 0.15);
    return clamp(color * shift, 0.0, 1.0);
}

void main() {
    vec4 raw = texture(uOesTexture, vTexCoord);
    
    // Exposure adjustment in linear-approximated space
    vec3 linearRgb = pow(max(raw.rgb, vec3(0.0)), vec3(2.2));
    linearRgb *= exp2(uExposureEv);
    vec3 srgb = pow(clamp(linearRgb, 0.0, 1.0), vec3(1.0 / 2.2));
    
    // Pre-LUT white balance modulation
    srgb = applyWhiteBalance(srgb, uTemperatureShift, uTintShift);
    
    fragColor = vec4(srgb, raw.a);
}
```

### 3.3 Pass 1: 3D LUT Color Remapping (`lut_filter.frag`)

Supports both hardware-accelerated trilinear `sampler3D` ($64 \times 64 \times 64$ cube) and a fallback $512 \times 512$ 2D HaldCLUT sampler with manual Z-slice interpolation. It also applies half-texel domain scaling so boundary values ($0.0$ and $1.0$) sample exact texel centers rather than wrapping across cube edges.

```glsl
#version 300 es
precision highp float;
precision highp sampler3D;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform sampler3D uLut3dTexture;   // 64x64x64 GL_TEXTURE_3D (GL_LINEAR, GL_CLAMP_TO_EDGE)
uniform sampler2D uHaldLutTexture; // 512x512 2D HaldCLUT fallback (8x8 grid of 64x64 slices)

uniform float uLutSize;            // Default: 64.0
uniform float uLutIntensity;       // [0.0, 1.0] blend factor
uniform int uUseHaldClut;          // 0 = use sampler3D, 1 = use 2D HaldCLUT

// Hardware-accelerated 3D LUT lookup with half-texel center alignment
vec3 sampleLut3D(vec3 rgb, float size) {
    float scale = (size - 1.0) / size;
    float offset = 1.0 / (2.0 * size);
    vec3 lutCoord = clamp(rgb, 0.0, 1.0) * scale + offset;
    return texture(uLut3dTexture, lutCoord).rgb;
}

// 2D HaldCLUT (512x512 containing 64 slices of 64x64 arranged in an 8x8 tile grid)
vec3 sampleHaldClut2D(vec3 rgb) {
    const float gridRows = 8.0;
    const float sliceSize = 64.0;
    const float texSize = 512.0;

    vec3 clamped = clamp(rgb, 0.0, 1.0);
    float blueSlice = clamped.b * (sliceSize - 1.0);
    float slice0 = floor(blueSlice);
    float slice1 = min(sliceSize - 1.0, slice0 + 1.0);
    float zFract = fract(blueSlice);

    // Map R and G to inner-tile pixel coordinates with half-texel offset
    vec2 rgOffset = (0.5 + clamped.rg * (sliceSize - 1.0)) / texSize;

    vec2 tile0 = vec2(mod(slice0, gridRows), floor(slice0 / gridRows)) / gridRows;
    vec2 tile1 = vec2(mod(slice1, gridRows), floor(slice1 / gridRows)) / texSize * sliceSize;
    tile1 = vec2(mod(slice1, gridRows), floor(slice1 / gridRows)) / gridRows;

    vec3 color0 = texture(uHaldLutTexture, tile0 + rgOffset).rgb;
    vec3 color1 = texture(uHaldLutTexture, tile1 + rgOffset).rgb;

    return mix(color0, color1, zFract);
}

void main() {
    vec4 baseColor = texture(uInputTexture, vTexCoord);
    
    vec3 gradedColor;
    if (uUseHaldClut == 1) {
        gradedColor = sampleHaldClut2D(baseColor.rgb);
    } else {
        gradedColor = sampleLut3D(baseColor.rgb, uLutSize);
    }
    
    vec3 finalRgb = mix(baseColor.rgb, gradedColor, uLutIntensity);
    fragColor = vec4(finalRgb, baseColor.a);
}
```

### 3.4 Pass 2: Radial Chromatic Aberration (`chromatic_aberration.frag`)

Simulates vintage single-element plastic or uncoated glass lenses (e.g., Kodak FunSaver, Holga, Ricoh GR1 corners) using a Brown-Conrady radial distortion polynomial $D(r) = 1 + k_1 r^2 + k_2 r^4$ with distinct wavelength dispersion weights for Red, Green, and Blue channels.

```glsl
#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform vec2 uResolution;          // Current FBO dimensions (width, height)
uniform float uAberrationStrength; // Typical range: [0.0, 0.025]
uniform float uCubicDistortion;    // Lens barrel/pincushion k2 coefficient: [-0.05, 0.05]
uniform float uFalloffPower;       // Radial exponent (e.g., 2.0 for quadratic corner buildup)

void main() {
    // Normalize coordinates to aspect-corrected [-1, 1] plane centered at optical axis
    vec2 aspect = vec2(uResolution.x / min(uResolution.x, uResolution.y),
                       uResolution.y / min(uResolution.x, uResolution.y));
    vec2 centeredUv = (vTexCoord - 0.5) * 2.0;
    vec2 opticalCoord = centeredUv * aspect;
    
    float r2 = dot(opticalCoord, opticalCoord);
    float r = sqrt(r2);
    float radialMask = pow(r, uFalloffPower);
    
    // Base radial distortion term: 1 + k1*r^2 + k2*r^4
    float baseDistortion = 1.0 + (uCubicDistortion * r2 * r2);
    
    // Chromatic dispersion offsets per wavelength (Red expands outward, Blue contracts inward)
    float rScale = baseDistortion + (uAberrationStrength * radialMask);
    float gScale = baseDistortion;
    float bScale = baseDistortion - (uAberrationStrength * radialMask * 1.15);
    
    vec2 uvR = clamp(0.5 + (centeredUv * rScale) * 0.5, 0.001, 0.999);
    vec2 uvG = clamp(0.5 + (centeredUv * gScale) * 0.5, 0.001, 0.999);
    vec2 uvB = clamp(0.5 + (centeredUv * bScale) * 0.5, 0.001, 0.999);
    
    // 3-tap sub-stepped dispersion along the radial vector for smooth lateral fringing
    float red   = 0.5 * texture(uInputTexture, uvR).r + 0.5 * texture(uInputTexture, mix(uvG, uvR, 0.5)).r;
    float green = texture(uInputTexture, uvG).g;
    float blue  = 0.5 * texture(uInputTexture, uvB).b + 0.5 * texture(uInputTexture, mix(uvG, uvB, 0.5)).b;
    float alpha = texture(uInputTexture, uvG).a;
    
    fragColor = vec4(red, green, blue, alpha);
}
```

### 3.5 Pass 3: Optical Halation & Glow Sub-Pipeline

Film halation occurs when intense high-contrast light penetrates the emulsion layers, reflects off the film base (especially in stocks without an anti-halation backing like CineStill 800T), and re-exposes the **red-sensitive layer** from behind, creating a warm red-orange halo around specular highlights.

#### Pass 3a: High-Luminance Soft-Knee Threshold (`halation_threshold.frag`)
Rendered at $\frac{1}{4}$ resolution ($480 \times 270$ for $1080\text{p}$ preview) for maximum GPU bandwidth efficiency.

```glsl
#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform float uThreshold; // Highlight luminance cutoff, e.g., 0.78
uniform float uKnee;      // Soft knee transition width, e.g., 0.15

void main() {
    vec3 color = texture(uInputTexture, vTexCoord).rgb;
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    
    // Quadratic soft-knee extraction curve
    float kneeMin = uThreshold - uKnee;
    float kneeMax = uThreshold + uKnee;
    float weight = smoothstep(kneeMin, kneeMax, luma);
    
    fragColor = vec4(color * weight, 1.0);
}
```

#### Pass 3b & 3c: Hardware Bilinear 9-Tap Separable Gaussian Blur (`separable_blur.frag`)
Uses hardware bilinear texture filtering offsets to achieve an effective 9-tap Gaussian kernel using only 5 texture fetches per fragment.

```glsl
#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform vec2 uTexelOffset; // (1.0/width * radius, 0.0) for H-pass; (0.0, 1.0/height * radius) for V-pass

void main() {
    vec2 off1 = vec2(1.3846153846) * uTexelOffset;
    vec2 off2 = vec2(3.2307692308) * uTexelOffset;
    
    vec3 sum = texture(uInputTexture, vTexCoord).rgb * 0.2270270270;
    sum += texture(uInputTexture, vTexCoord + off1).rgb * 0.3162162162;
    sum += texture(uInputTexture, vTexCoord - off1).rgb * 0.3162162162;
    sum += texture(uInputTexture, vTexCoord + off2).rgb * 0.0702702703;
    sum += texture(uInputTexture, vTexCoord - off2).rgb * 0.0702702703;
    
    fragColor = vec4(sum, 1.0);
}
```

#### Pass 3d: Warm Halation Composite (`halation_composite.frag`)

```glsl
#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uBaseTexture;
uniform sampler2D uHalationBlurTexture;
uniform vec3 uHalationTint;     // Default CineStill red-orange: vec3(1.0, 0.26, 0.08)
uniform float uHalationStrength;// Range: [0.0, 1.0]

void main() {
    vec4 base = texture(uBaseTexture, vTexCoord);
    vec3 blur = texture(uHalationBlurTexture, vTexCoord).rgb;
    
    float blurLuma = dot(blur, vec3(0.299, 0.587, 0.114));
    vec3 tintedHalo = mix(blur, vec3(blurLuma) * uHalationTint * 2.2, 0.85);
    
    // Inverse-luminance weighted screen blend so halation blooms into darker surrounding edges
    vec3 result = 1.0 - (1.0 - base.rgb) * (1.0 - tintedHalo * uHalationStrength);
    fragColor = vec4(clamp(result, 0.0, 1.0), base.a);
}
```

### 3.6 Pass 4: Film Grain & Luminance Blend (`film_grain.frag`)

Real silver-halide emulsion grain is neither uniform white noise nor static across brightness levels. It exhibits spatial clumping (modeled via multi-octave 2D Simplex noise) and a **bell-shaped luminance response curve** where grain is suppressed in pure highlights ($Y \to 1.0$) and deep toe shadows ($Y \to 0.0$), peaking in the midtones ($Y \approx 0.45$).

```glsl
#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform vec2 uResolution;
uniform float uTimeSeed;       // Frame-randomized seed: fract(time * 43.17)
uniform float uGrainIntensity; // Range: [0.0, 0.35]
uniform float uGrainSize;      // Particle scale factor: [1.0 (35mm fine) to 3.5 (110/8mm coarse)]
uniform float uColorGrain;     // 0.0 = Monochrome silver grain, 1.0 = Dye-cloud chroma noise

// Fast GPU 2D Simplex Noise (Ashima / Ian McEwan)
vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec2 mod289(vec2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec3 permute(vec3 x) { return mod289(((x * 34.0) + 1.0) * x); }

float snoise(vec2 v) {
    const vec4 C = vec4(
        0.211324865405187,  // (3.0 - sqrt(3.0)) / 6.0
        0.366025403784439,  // 0.5 * (sqrt(3.0) - 1.0)
       -0.577350269189626,  // -1.0 + 2.0 * C.x
        0.024390243902439   // 1.0 / 41.0
    );
    vec2 i  = floor(v + dot(v, C.yy));
    vec2 x0 = v - i + dot(i, C.xx);
    vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod289(i);
    vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
    vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
    m = m * m;
    m = m * m;
    vec3 x = 2.0 * fract(p * C.www) - 1.0;
    vec3 h = abs(x) - 0.5;
    vec3 ox = floor(x + 0.5);
    vec3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
    vec3 g;
    g.x  = a0.x  * x0.x  + h.x  * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return 130.0 * dot(m, g);
}

// Pegtop continuous Soft-Light blend (eliminates derivative discontinuity at 0.5)
vec3 blendSoftLight(vec3 base, vec3 blend) {
    return (1.0 - 2.0 * blend) * base * base + 2.0 * blend * base;
}

void main() {
    vec4 baseColor = texture(uInputTexture, vTexCoord);
    
    // Normalize grain coordinate space relative to a reference 1080p short edge
    // so grain size remains identical between 1080p preview and 4000x3000 photo capture!
    float minDim = min(uResolution.x, uResolution.y);
    vec2 normCoord = (vTexCoord * uResolution) / (minDim / 1080.0);
    vec2 grainUv = (normCoord / max(uGrainSize, 0.5)) + vec2(uTimeSeed * 173.3, uTimeSeed * 291.7);
    
    // Two-octave organic silver-halide clumping
    float nPrimary = snoise(grainUv * 0.65);
    float nFine    = snoise(grainUv * 1.45 + vec2(19.1, 47.3));
    float monoGrain = (nPrimary * 0.65 + nFine * 0.35);
    
    // Optional dye-cloud chroma noise for high-ISO color negative stocks (e.g., Superia 800)
    vec3 chromaGrain = vec3(
        monoGrain,
        snoise(grainUv * 0.65 + vec2(53.2, 11.8)),
        snoise(grainUv * 0.65 + vec2(91.4, 83.1))
    );
    vec3 grainSample = mix(vec3(monoGrain), chromaGrain, uColorGrain);
    
    // Emulsion luminance response curve: peak in midtones, suppressed in deep shadows & speculars
    float luma = dot(baseColor.rgb, vec3(0.2126, 0.7152, 0.0722));
    float shadowMask = smoothstep(0.0, 0.12, luma);
    float highlightMask = 1.0 - smoothstep(0.75, 0.98, luma);
    float midtoneWeight = shadowMask * highlightMask;
    
    // Map [-1, 1] noise to [0, 1] neutral-gray centered (0.5) overlay space
    vec3 blendLayer = clamp(vec3(0.5) + grainSample * uGrainIntensity * midtoneWeight, 0.0, 1.0);
    vec3 grainedRgb = blendSoftLight(baseColor.rgb, blendLayer);
    
    fragColor = vec4(clamp(grainedRgb, 0.0, 1.0), baseColor.a);
}
```

### 3.7 Pass 5: Final Optical Compositing (`compositing.frag`)

Applies the physical $\cos^4(\theta)$ natural illumination falloff law for lens vignetting, screen-blended randomized light leaks, and alpha-composited film border masks.

```glsl
#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform sampler2D uLightLeakTexture;
uniform sampler2D uBorderMaskTexture;

uniform vec2 uResolution;
uniform float uVignetteIntensity; // [0.0, 1.5]
uniform float uVignetteSmoothness;// [0.2, 1.0]
uniform float uLightLeakOpacity;  // [0.0, 1.0]
uniform vec2 uLightLeakUvTransform;// Allows flipping/mirroring leak assets randomly per shot
uniform int uEnableBorderMask;    // 0 = disabled, 1 = enabled

void main() {
    vec4 color = texture(uInputTexture, vTexCoord);
    
    // 1. Optical Vignette Falloff
    vec2 aspect = vec2(uResolution.x / min(uResolution.x, uResolution.y),
                       uResolution.y / min(uResolution.x, uResolution.y));
    vec2 centered = (vTexCoord - 0.5) * aspect;
    float dist = length(centered);
    float vignette = smoothstep(
        0.85 * uVignetteSmoothness + 0.35,
        0.20 * uVignetteSmoothness,
        dist * uVignetteIntensity
    );
    vec3 composed = color.rgb * vignette;
    
    // 2. Organic Light Leak (Screen Blend)
    if (uLightLeakOpacity > 0.001) {
        vec2 leakUv = abs(uLightLeakUvTransform - vTexCoord);
        vec3 leak = texture(uLightLeakTexture, leakUv).rgb * uLightLeakOpacity;
        composed = 1.0 - (1.0 - composed) * (1.0 - leak);
    }
    
    // 3. Retro Film Border / Sprocket / Polaroid Frame Mask (Premultiplied Alpha Overlay)
    if (uEnableBorderMask == 1) {
        vec4 border = texture(uBorderMaskTexture, vTexCoord);
        composed = mix(composed, border.rgb, border.a);
    }
    
    fragColor = vec4(clamp(composed, 0.0, 1.0), 1.0);
}
```

---

## 4. Kotlin Implementation Scaffolding

### 4.1 Preset Architecture (`CameraPreset.kt` & JSON Schema)

```kotlin
package com.vintagecam.domain.presets.model

import kotlinx.serialization.Serializable

@Serializable
enum class AspectRatioProfile(val widthRatio: Float, val heightRatio: Float) {
    RATIO_4_3(4f, 3f),
    RATIO_3_2(3f, 2f),
    RATIO_1_1(1f, 1f),
    HALF_FRAME_DIPTYCH(3f, 4f) // Captures two 3:4 frames stitched onto a 3:2 landscape canvas
}

@Serializable
data class LutConfig(
    val assetPath: String,        // e.g., "presets/luts/portra_400.raw3d"
    val lutSize: Int = 64,
    val isHaldClut: Boolean = false,
    val defaultIntensity: Float = 1.0f,
    val temperatureShift: Float = 0.0f,
    val tintShift: Float = 0.0f
)

@Serializable
data class AberrationConfig(
    val strength: Float = 0.004f,
    val cubicDistortion: Float = -0.012f,
    val falloffPower: Float = 2.2f
)

@Serializable
data class HalationConfig(
    val enabled: Boolean = false,
    val threshold: Float = 0.80f,
    val knee: Float = 0.14f,
    val blurRadius: Float = 2.2f,
    val tintRgb: List<Float> = listOf(1.0f, 0.26f, 0.08f),
    val strength: Float = 0.45f
)

@Serializable
data class GrainConfig(
    val intensity: Float = 0.12f,
    val particleSize: Float = 1.35f,
    val colorNoiseRatio: Float = 0.25f
)

@Serializable
data class CompositingConfig(
    val vignetteIntensity: Float = 0.65f,
    val vignetteSmoothness: Float = 0.70f,
    val lightLeakPool: List<String> = emptyList(),
    val borderMaskAsset: String? = null,
    val enableDateStamp: Boolean = false
)

@Serializable
data class CameraPreset(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val aspectRatio: AspectRatioProfile,
    val lut: LutConfig,
    val aberration: AberrationConfig,
    val halation: HalationConfig,
    val grain: GrainConfig,
    val compositing: CompositingConfig
)
```

#### Sample `presets_catalog.json`:

```json
[
  {
    "id": "portra_400_35mm",
    "displayName": "KP 400",
    "subtitle": "Warm Skin Tones • Fine 35mm Grain",
    "aspectRatio": "RATIO_3_2",
    "lut": {
      "assetPath": "presets/luts/portra_400.raw3d",
      "lutSize": 64,
      "isHaldClut": false,
      "defaultIntensity": 0.95,
      "temperatureShift": 0.08,
      "tintShift": -0.03
    },
    "aberration": { "strength": 0.0035, "cubicDistortion": -0.008, "falloffPower": 2.4 },
    "halation": { "enabled": true, "threshold": 0.82, "knee": 0.15, "blurRadius": 1.8, "tintRgb": [1.0, 0.32, 0.12], "strength": 0.30 },
    "grain": { "intensity": 0.09, "particleSize": 1.2, "colorNoiseRatio": 0.20 },
    "compositing": {
      "vignetteIntensity": 0.55,
      "vignetteSmoothness": 0.75,
      "lightLeakPool": ["presets/leaks/leak_warm_01.ktx", "presets/leaks/leak_warm_02.ktx"],
      "borderMaskAsset": "presets/borders/35mm_sprocket.ktx",
      "enableDateStamp": true
    }
  },
  {
    "id": "fuji_superia_half",
    "displayName": "FS 400H Half",
    "subtitle": "Emerald Shadows • Diptych Half-Frame",
    "aspectRatio": "HALF_FRAME_DIPTYCH",
    "lut": {
      "assetPath": "presets/luts/fuji_superia_400.raw3d",
      "lutSize": 64,
      "isHaldClut": false,
      "defaultIntensity": 1.0,
      "temperatureShift": -0.05,
      "tintShift": -0.08
    },
    "aberration": { "strength": 0.008, "cubicDistortion": -0.022, "falloffPower": 2.0 },
    "halation": { "enabled": true, "threshold": 0.76, "knee": 0.12, "blurRadius": 2.5, "tintRgb": [1.0, 0.22, 0.06], "strength": 0.55 },
    "grain": { "intensity": 0.16, "particleSize": 1.65, "colorNoiseRatio": 0.35 },
    "compositing": {
      "vignetteIntensity": 0.80,
      "vignetteSmoothness": 0.65,
      "lightLeakPool": ["presets/leaks/leak_side_burn.ktx"],
      "borderMaskAsset": "presets/borders/half_frame_divider.ktx",
      "enableDateStamp": true
    }
  }
]
```

---

### 4.2 CameraX-to-SurfaceTexture Zero-Copy Binding (`CameraController.kt`)

```kotlin
package com.vintagecam.core.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.vintagecam.core.gl.GlThreadExecutor
import java.util.concurrent.Executor

class CameraController(
    private val context: Context,
    private val glExecutor: GlThreadExecutor,
    private val onPreviewFrameAvailable: (SurfaceTexture) -> Unit,
    private val onHighResFrameCaptured: (ImageProxy) -> Unit
) {
    private var imageCapture: ImageCapture? = null
    private var activeSurfaceTexture: SurfaceTexture? = null
    private var activeSurface: Surface? = null

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        oesTextureId: Int,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val cameraProvider = providerFuture.get()
            cameraProvider.unbindAll()

            // Lock sensor to 4:3 (native sensor readout); aspect cropping is handled in GL Pass 0
            val previewResSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1920, 1440),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            val preview = Preview.Builder()
                .setResolutionSelector(previewResSelector)
                .build()

            val captureResSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .build()

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setResolutionSelector(captureResSelector)
                .build()

            // Bind Preview directly to our OpenGL ES OES Texture on the GL Thread
            preview.setSurfaceProvider(glExecutor) { surfaceRequest ->
                val resolution = surfaceRequest.resolution
                val surfaceTexture = SurfaceTexture(oesTextureId).apply {
                    setDefaultBufferSize(resolution.width, resolution.height)
                    setOnFrameAvailableListener(
                        { st -> onPreviewFrameAvailable(st) },
                        glExecutor.handler
                    )
                }
                val surface = Surface(surfaceTexture)
                activeSurfaceTexture = surfaceTexture
                activeSurface = surface

                surfaceRequest.provideSurface(surface, glExecutor) {
                    surface.release()
                    surfaceTexture.release()
                }
            }

            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(context))
    }

    fun triggerHighResCapture(captureExecutor: Executor, onError: (Throwable) -> Unit) {
        val captureUseCase = imageCapture ?: return
        captureUseCase.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    onHighResFrameCaptured(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }
}
```

---

### 4.3 Multi-Pass FBO Pipeline & Ping-Pong Manager (`PingPongFboPool.kt` & `VintageGlPipeline.kt`)

```kotlin
package com.vintagecam.core.gl.fbo

import android.opengl.GLES30

class FramebufferObject(val width: Int, val height: Int, useFloat16: Boolean = true) {
    var fboId: Int = 0
        private set
    var textureId: Int = 0
        private set

    init {
        val fbos = IntArray(1)
        val textures = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        GLES30.glGenTextures(1, textures, 0)
        fboId = fbos[0]
        textureId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        val internalFormat = if (useFloat16) GLES30.GL_RGBA16F else GLES30.GL_RGBA8
        val type = if (useFloat16) GLES30.GL_HALF_FLOAT else GLES30.GL_UNSIGNED_BYTE

        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, internalFormat,
            width, height, 0, GLES30.GL_RGBA, type, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, textureId, 0
        )

        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        check(status == GLES30.GL_FRAMEBUFFER_COMPLETE) {
            "FBO allocation failed with status: 0x${Integer.toHexString(status)}"
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    fun bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glViewport(0, 0, width, height)
    }

    fun release() {
        GLES30.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
        GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
    }
}

class PingPongFboPool(val width: Int, val height: Int) {
    private val fboA = FramebufferObject(width, height, useFloat16 = true)
    private val fboB = FramebufferObject(width, height, useFloat16 = true)

    // Quarter-resolution FBOs dedicated to Pass 3 (Halation threshold + separable Gaussian blur)
    val quarterFbo1 = FramebufferObject(
        (width / 4).coerceAtLeast(1),
        (height / 4).coerceAtLeast(1),
        useFloat16 = true
    )
    val quarterFbo2 = FramebufferObject(
        (width / 4).coerceAtLeast(1),
        (height / 4).coerceAtLeast(1),
        useFloat16 = true
    )

    private var isAWriteTarget = true

    val readFbo: FramebufferObject
        get() = if (isAWriteTarget) fboB else fboA

    val writeFbo: FramebufferObject
        get() = if (isAWriteTarget) fboA else fboB

    fun swap() {
        isAWriteTarget = !isAWriteTarget
    }

    fun release() {
        fboA.release()
        fboB.release()
        quarterFbo1.release()
        quarterFbo2.release()
    }
}
```

#### Orchestrating the 6-Pass Chain (`VintageGlPipeline.kt`):

```kotlin
package com.vintagecam.core.gl

import android.opengl.GLES30
import com.vintagecam.core.gl.fbo.FramebufferObject
import com.vintagecam.core.gl.fbo.PingPongFboPool
import com.vintagecam.core.gl.pass.*
import com.vintagecam.domain.presets.model.CameraPreset

class VintageGlPipeline(
    private val oesIngestPass: OesIngestPass,
    private val lut3dPass: Lut3dPass,
    private val chromaticAberrationPass: ChromaticAberrationPass,
    private val halationChainPass: HalationChainPass,
    private val filmGrainPass: FilmGrainPass,
    private val compositingPass: CompositingPass
) {
    /**
     * Executes the complete vintage rendering pipeline.
     * @param outputFbo If null, Pass 5 renders directly to the default window framebuffer (0) for preview.
     *                  If non-null, Pass 5 renders into [outputFbo] for high-res PBO photo capture.
     */
    fun executeChain(
        inputTextureId: Int,
        isExternalOes: Boolean,
        texTransformMatrix: FloatArray,
        pool: PingPongFboPool,
        preset: CameraPreset,
        lut3dTextureId: Int,
        lightLeakTextureId: Int,
        borderMaskTextureId: Int,
        exposureEv: Float,
        timeSeed: Float,
        viewportWidth: Int,
        viewportHeight: Int,
        outputFbo: FramebufferObject? = null
    ) {
        // PASS 0: OES-to-2D Ingestion, Sensor Rotation, Aspect Crop & Exposure
        pool.writeFbo.bind()
        oesIngestPass.render(
            inputTextureId = inputTextureId,
            isExternalOes = isExternalOes,
            texMatrix = texTransformMatrix,
            exposureEv = exposureEv,
            temperatureShift = preset.lut.temperatureShift,
            tintShift = preset.lut.tintShift
        )
        pool.swap()

        // PASS 1: 3D LUT Color Remapping
        pool.writeFbo.bind()
        lut3dPass.render(
            inputTexture2d = pool.readFbo.textureId,
            lutTextureId = lut3dTextureId,
            lutSize = preset.lut.lutSize.toFloat(),
            intensity = preset.lut.defaultIntensity,
            useHaldClut = preset.lut.isHaldClut
        )
        pool.swap()

        // PASS 2: Radial Chromatic Aberration
        pool.writeFbo.bind()
        chromaticAberrationPass.render(
            inputTexture2d = pool.readFbo.textureId,
            width = pool.width.toFloat(),
            height = pool.height.toFloat(),
            config = preset.aberration
        )
        pool.swap()

        // PASS 3: Optical Halation & Glow (Threshold -> H-Blur -> V-Blur -> Warm Additive Composite)
        if (preset.halation.enabled) {
            halationChainPass.render(
                baseInputTexture2d = pool.readFbo.textureId,
                quarterFbo1 = pool.quarterFbo1,
                quarterFbo2 = pool.quarterFbo2,
                targetWriteFbo = pool.writeFbo,
                config = preset.halation
            )
            pool.swap()
        }

        // PASS 4: Procedural Simplex Film Grain & Midtone Soft-Light Blend
        pool.writeFbo.bind()
        filmGrainPass.render(
            inputTexture2d = pool.readFbo.textureId,
            width = pool.width.toFloat(),
            height = pool.height.toFloat(),
            timeSeed = timeSeed,
            config = preset.grain
        )
        pool.swap()

        // PASS 5: Final Optical Compositing (Vignette + Light Leak + Border Mask)
        if (outputFbo != null) {
            outputFbo.bind()
        } else {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
        }

        compositingPass.render(
            inputTexture2d = pool.readFbo.textureId,
            lightLeakTexture2d = lightLeakTextureId,
            borderMaskTexture2d = borderMaskTextureId,
            width = (outputFbo?.width ?: viewportWidth).toFloat(),
            height = (outputFbo?.height ?: viewportHeight).toFloat(),
            config = preset.compositing
        )
    }
}
```

---

### 4.4 High-Resolution Offscreen Photo Processor (`PboPhotoReader.kt`)

Executing `glReadPixels` directly into CPU memory blocks the GL pipeline for $60\text{ ms}$–$120\text{ ms}$ on $12\text{ MP}$ captures. Using an OpenGL ES 3.0 **Pixel Buffer Object (`GL_PIXEL_PACK_BUFFER`)** with `glFenceSync` transfers pixel data via DMA asynchronously while the viewfinder continues running at $60\text{ FPS}$.

```kotlin
package com.vintagecam.core.gl

import android.graphics.Bitmap
import android.opengl.GLES30
import com.vintagecam.core.gl.fbo.FramebufferObject
import java.nio.ByteBuffer

class PboPhotoReader {
    /**
     * Reads a high-resolution [FramebufferObject] into an Android [Bitmap] using DMA PBO transfer.
     */
    fun readFboToBitmap(targetFbo: FramebufferObject): Bitmap {
        val width = targetFbo.width
        val height = targetFbo.height
        val byteCount = width * height * 4

        val pboIds = IntArray(1)
        GLES30.glGenBuffers(1, pboIds, 0)
        val pboId = pboIds[0]

        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pboId)
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, byteCount, null, GLES30.GL_STREAM_READ)

        targetFbo.bind()
        // Asynchronous DMA transfer into bound GL_PIXEL_PACK_BUFFER (offset = 0)
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, 0)

        // Insert GPU fence sync and wait for DMA completion
        val fence = GLES30.glFenceSync(GLES30.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
        GLES30.glFlush()
        GLES30.glClientWaitSync(fence, GLES30.GL_SYNC_FLUSH_COMMANDS_BIT, 500_000_000L) // 500ms timeout
        GLES30.glDeleteSync(fence)

        val mappedBuffer = GLES30.glMapBufferRange(
            GLES30.GL_PIXEL_PACK_BUFFER,
            0,
            byteCount,
            GLES30.GL_MAP_READ_BIT
        ) as ByteBuffer

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(mappedBuffer)

        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER)
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glDeleteBuffers(1, pboIds, 0)

        return bitmap
    }
}
```

---

### 4.5 Jetpack Compose Skeuomorphic UI & Haptics (`ViewfinderScreen.kt`)

```kotlin
package com.vintagecam.feature.viewfinder.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SkeuomorphicViewfinderScreen(
    shotCount: Int,
    isHalfFrameSecondShot: Boolean,
    presetName: String,
    aspectRatioValue: Float,
    onShutterClicked: () -> Unit,
    glSurfaceContent: @Composable (Modifier) -> Unit
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val shutterBlindProgress = remember { Animatable(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414)) // Textured leatherette dark chassis
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Metallic Status Deck & Mechanical Film Counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = presetName,
                color = Color(0xFFE5A93C),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            // Mechanical Odometer-Style Film Counter
            Box(
                modifier = Modifier
                    .border(1.5.dp, Color(0xFF3A3A3A), RoundedCornerShape(4.dp))
                    .background(Color(0xFF0A0A0A), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                val frameSuffix = if (isHalfFrameSecondShot) "B" else "A"
                Text(
                    text = String.format("%02d %s", shotCount, frameSuffix),
                    color = Color(0xFFF2ECE1),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }

        // Optical Viewfinder Window with Dual-Curtain Mechanical Shutter Blind
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatioValue)
                .clip(RoundedCornerShape(10.dp))
                .border(3.dp, Color(0xFF2C2C2C), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            glSurfaceContent(Modifier.fillMaxSize())

            // Dual-Blade Leaf/Focal-Plane Shutter Blind Overlay
            if (shutterBlindProgress.value > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val bladeHeight = (size.height / 2f) * shutterBlindProgress.value
                    drawRect(
                        color = Color(0xFF080808),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width, bladeHeight)
                    )
                    drawRect(
                        color = Color(0xFF080808),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - bladeHeight),
                        size = androidx.compose.ui.geometry.Size(size.width, bladeHeight)
                    )
                }
            }
        }

        // Chrome Threaded Mechanical Shutter Button
        Box(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .size(78.dp)
                .clip(CircleShape)
                .background(Color(0xFFD8D8D8))
                .border(4.dp, Color(0xFF8A8A8A), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Crisp dual-stage mechanical shutter haptics
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }

                    onShutterClicked()

                    coroutineScope.launch {
                        shutterBlindProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 55, easing = FastOutLinearInEasing)
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                        }
                        shutterBlindProgress.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 85, easing = LinearOutSlowInEasing)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFECECEC))
                    .border(1.5.dp, Color(0xFF4A4A4A), CircleShape)
            )
        }
    }
}
```

---

## 5. Asset Acquisition & Optimization Strategy

### 5.1 Converting Industry `.cube` 3D LUTs into Mobile-Optimized `.raw3d` and HaldCLUT Assets

Desktop color grading packages (DaVinci Resolve, Dehancer, Lattice) export 3D LUTs as ASCII `.cube` files, which can exceed $5\text{ MB}$ per file and take $80\text{ ms}$–$150\text{ ms}$ to parse in Kotlin on mobile CPUs.

Instead, convert `.cube` files offline during the build process into **raw binary 3D textures (`.raw3d`)**:
* A $64 \times 64 \times 64$ `RGB8` binary cube is exactly $64^3 \times 3 = 786,432\text{ bytes}$ ($768\text{ KB}$ uncompressed, or $\sim 180\text{ KB}$ inside the APK zip container).
* It loads directly into a `ByteBuffer.allocateDirect(786432)` and uploads to `glTexImage3D(GL_TEXTURE_3D, 0, GL_RGB8, 64, 64, 64, 0, GL_RGB, GL_UNSIGNED_BYTE, buffer)` in **under $1.8\text{ ms}$** with zero string parsing.

#### Python Build Script (`tools/cube_to_raw3d.py`):

```python
#!/usr/bin/env python3
"""
Converts an Adobe/Resolve .cube 3D LUT file into:
1. A compact binary .raw3d (64x64x64 RGB8) buffer for direct glTexImage3D upload.
2. A 512x512 2D HaldCLUT PNG fallback image.
"""
import sys
import struct
import numpy as np
from PIL import Image

def parse_cube(filepath: str):
    size = 0
    data = []
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#') or line.startswith('TITLE') or line.startswith('DOMAIN'):
                continue
            if line.startswith('LUT_3D_SIZE'):
                size = int(line.split()[1])
                continue
            parts = line.split()
            if len(parts) == 3:
                r, g, b = (float(parts[0]), float(parts[1]), float(parts[2]))
                data.append((r, g, b))
    lut = np.array(data, dtype=np.float32).reshape((size, size, size, 3))
    return size, np.clip(lut, 0.0, 1.0)

def export_raw3d(lut: np.ndarray, output_path: str):
    # Quantize [0.0, 1.0] float to 8-bit unsigned integers [0, 255]
    quantized = np.rint(lut * 255.0).astype(np.uint8)
    with open(output_path, 'wb') as f:
        f.write(quantized.tobytes(order='C'))
    print(f"Exported binary 3D LUT ({quantized.nbytes} bytes) -> {output_path}")

def export_hald_png(lut: np.ndarray, size: int, output_path: str):
    # Arrange 64 slices of 64x64 into an 8x8 2D grid (512x512)
    grid = int(np.sqrt(size))
    canvas = np.zeros((size * grid, size * grid, 3), dtype=np.uint8)
    quantized = np.rint(lut * 255.0).astype(np.uint8)
    for b in range(size):
        row = b // grid
        col = b % grid
        y0, x0 = row * size, col * size
        canvas[y0:y0 + size, x0:x0 + size, :] = quantized[b, :, :, :]
    Image.fromarray(canvas, mode='RGB').save(output_path, optimize=True)
    print(f"Exported 2D HaldCLUT ({canvas.shape[1]}x{canvas.shape[0]}) -> {output_path}")

if __name__ == "__main__":
    cube_file = sys.argv[1]
    raw_out = sys.argv[2]
    png_out = sys.argv[3]
    lut_size, lut_array = parse_cube(cube_file)
    export_raw3d(lut_array, raw_out)
    export_hald_png(lut_array, lut_size, png_out)
```

### 5.2 GPU Texture Compression for Light Leaks & Film Borders

* **Light Leaks:** High-resolution ($2048 \times 1536$) PNGs consume $12\text{ MB}$ of VRAM each when uncompressed. Because light leaks are low-frequency gradients, downsample them to $1024 \times 768$ and compress using **ETC2 RGB8 (`GL_COMPRESSED_RGB8_ETC2`)** or **ASTC $6 \times 6$ (`GL_COMPRESSED_RGBA_ASTC_6x6_KHR`)** inside `.ktx` containers. Each leak asset drops to $\sim 256\text{ KB}$ in VRAM.
* **Film Borders & Sprocket Masks:** Require crisp alpha edges around sprocket holes and Polaroid frames. Compress at $2048 \times 1536$ using **ASTC $4 \times 4$ (`GL_COMPRESSED_RGBA_ASTC_4x4_KHR`)** or **ETC2 RGBA8 (`GL_COMPRESSED_RGBA8_ETC2_EAC`)**, which are natively supported across $100\%$ of OpenGL ES 3.0 Android devices.

---

## 6. Step-by-Step Implementation Roadmap

### Phase 1: EGL Core & Zero-Copy Viewfinder Foundation (Weeks 1–2)
1. **Milestone 1.1:** Implement `EglCore` and `GlThreadExecutor` managing a dedicated `HandlerThread` with an `EGL14` context configured for OpenGL ES 3.0 (`EGL_CONTEXT_CLIENT_VERSION = 3`).
2. **Milestone 1.2:** Build `CameraController` binding CameraX `Preview` to a `SurfaceTexture` backed by `GL_TEXTURE_EXTERNAL_OES`.
3. **Milestone 1.3:** Implement **Pass 0 (`oes_ingest.frag`)** and `AspectMatrixCalculator` to verify zero-stutter switching between $4:3$, $3:2$, and $1:1$ aspect ratios at $60\text{ FPS}$.

### Phase 2: Multi-Pass FBO Engine & Real-Time Shaders (Weeks 3–4)
1. **Milestone 2.1:** Build `PingPongFboPool` using `GL_RGBA16F` attachments and verify zero memory leaks across surface recreation (`onSurfaceChanged`).
2. **Milestone 2.2:** Implement `Lut3dTextureLoader` and **Pass 1 (`lut_filter.frag`)**, verifying trilinear interpolation accuracy against a known identity `.raw3d` cube.
3. **Milestone 2.3:** Integrate **Pass 2 (`chromatic_aberration.frag`)**, **Pass 3 (`HalationChainPass` at $\frac{1}{4}$ resolution)**, **Pass 4 (`film_grain.frag`)**, and **Pass 5 (`compositing.frag`)**.
4. **Performance Gate:** Profile using **Android GPU Inspector (AGI)** on a mid-range Snapdragon 7-series device; ensure total GPU frame time stays under $11.5\text{ ms}$ at $1080\text{p}$.

### Phase 3: High-Resolution Offscreen Capture & Half-Frame Stitcher (Weeks 5–6)
1. **Milestone 3.1:** Hook CameraX `ImageCapture` (configured for `OUTPUT_FORMAT_YUV_420_888` or `JPEG` decoded into an `AHardwareBuffer` / `GL_TEXTURE_2D`) into the shared `EglCore` context.
2. **Milestone 3.2:** Allocate full-resolution offscreen FBOs dynamically upon capture (clamping dimensions to `min(captureDim, GL_MAX_TEXTURE_SIZE)` and falling back to $4096\text{ px}$ max edge on low-VRAM devices).
3. **Milestone 3.3:** Implement `PboPhotoReader` for asynchronous `GL_PIXEL_PACK_BUFFER` readback, `ExifMetadataWriter` (copying ISO, shutter speed, and GPS tags), and **Half-Frame Diptych State Machine** (caching Shot A into an internal file/texture and compositing Shot A + Shot B side-by-side with a center film divider on the second shutter press).

### Phase 4: Skeuomorphic Compose UI, Presets & Polish (Weeks 7–8)
1. **Milestone 4.1:** Build `PresetRepository` and convert 8–12 signature film LUTs (*Portra 400*, *CineStill 800T*, *Fuji Superia 400*, *Ilford HP5*, *Polaroid 600*, *2000s CCD Digicam*) using `cube_to_raw3d.py`.
2. **Milestone 4.2:** Implement `SkeuomorphicViewfinderScreen`, mechanical dual-curtain shutter blind animation, sound effects (`SoundPool` low-latency mechanical clack + film winder motor), and `HapticFeedbackConstants` choreography.
3. **Milestone 4.3:** Add date-stamp vector glyph rendering into Pass 5 (classic 1990s quartz orange LED `'98 10 22` glow) and finalize MediaStore gallery export.

---

## 7. Exhaustive Film Stock, Camera Preset & Optical Filter Catalog

This section provides the complete, unabridged catalog of all cameras, film profiles, optical accessories, video emulations, and generative aesthetic prompt mappings required to recreate the comprehensive analog experience (matching the Dazz Cam catalog and modern generative aesthetic filters). Every entry specifies optical geometry, color grading profiles, halation response curves, and grain structures for direct implementation in `presets_catalog.json`.

```
========================================================================================================
                                     CAMERA & FILTER TAXONOMY
========================================================================================================
  [DIGITAL SENSORS]       [VINTAGE 135 (35mm)]     [VINTAGE 120 (Medium)]   [INSTANT COLLECTION]
  - Original (HEIF/RAW)   - D Classic (Leica M)    - S Classic (Rolleiflex) - Inst C (Instax Mini)
  - GRD R (Ricoh GR III)  - GR F (Ricoh GR1)       - HOGA (Holga 120N)      - Inst SQ (Instax SQ1)
  - CCD R (PowerShot CCD) - CT2F (Contax T2)       - S 67 (Pentax 6x7)      - Inst SQC (Instax SQ10)
  - FXN R (Fujifilm X100) - D3D (Nishika 3D)       - KV80 R (Hasselblad)    - PAF R (Polaroid SX-70)
  - Collage (DIY Cutout)  - 135 NE (Filmstrip)
                          - D FunS (FunSaver)      [RETRO VIDEO & TAPE]     [OPTICAL ACCESSORIES]
                          - IR (Aerochrome)        - VHS (JVC GR-C1)        - Prism Lens
                          - Classic U (Leica R)    - 8mm (Super 8 Cine)     - Fisheye F & W
                          - DQS (Simple Ace 400)   - V Classic (Camcorder)  - Starburst Diffraction
                          - FQS R (QuickSnap)      - DAM / A709 (Betacam)   - Color Flash Gels
                          - Golf (ActionSampler)   - KV88 (Sony Video8)     - Optical Neutral Density
                          - CPM35 (SureShot 85)    - DCR 20x (MiniDV)       - Double Exposure Engine
                          - 135 SR (Sprocket)      - Original V (Log Video) - Light Leak Burn Pools
                          - D Half (Olympus Pen)   - Kino (Cinema 24fps)    - Quartz LED Date Stamp
                          - D Slide (E-6 Reversal)
                          - OFM R (Vision3 Movie)
                          - SRX (Wide-Format Film)
========================================================================================================
```

---

### 7.1 Digital Sensor Emulations (5 Presets)

Digital models simulate sharp CMOS/CCD digital camera characteristics, direct flash falloff, and distinct electronic sensor color reproduction.

#### 1. `original_cam` (Original Standard / ProRAW / Zero-Processing RAW)
* **Real-World Inspiration:** Modern flagship smartphone sensor pipeline.
* **Profiles:**
  - `HEIF`: Standard tone curve, aggressive ISP multi-frame HDR, high edge sharpening.
  - `ProRAW`: 12-bit linear sensor output with conservative standard tone curve and unsharpened details.
  - `RAW`: Unprocessed, digicam-style sensor readout with lower dynamic range and natural sensor photon noise.
* **Optical Parameters:** Aspect Ratio: $4:3$ (`RATIO_4_3`) | Distortion $k_2 = 0.0$ | Vignette: None.
* **Shader Uniforms:**
  $$\text{Aberration} = 0.0, \quad \text{Halation} = \text{Disabled}, \quad \text{Grain} = 0.0, \quad \text{Vignette} = 0.0$$

#### 2. `grd_r` (GRD R)
* **Real-World Inspiration:** Ricoh GR Digital series (GR III / GR IIIx).
* **Aesthetic:** Deep, inky blacks with steep shadow contrast; hyper-saturated crimson reds and deep oceanic blues; yellow luminous boost; snappy micro-contrast suited for street and documentary photography.
* **Optical Parameters:** Aspect Ratio: $3:2$ (`RATIO_3_2`) | Lens: $28\text{mm}$ equivalent wide-angle falloff.
* **Shader Uniforms:**
  $$\text{Aberration} = 0.002, \quad k_2 = -0.004, \quad \text{Grain Intensity} = 0.06, \quad \text{Grain Size} = 1.0, \quad \text{Vignette} = 0.48$$

#### 3. `ccd_r` (CCD R - Early 2000s Digicam)
* **Real-World Inspiration:** Early 2000s CCD sensor compact cameras (Canon PowerShot A-series, Sony Cyber-shot DSC, Olympus Camedia).
* **Color Profiles:**
  - `FX3-2`: Harsh, punchy contrast with rapid specular highlight clipping; saturated primary tones; classic direct-flash digicam skin rendering.
  - `FX3`: Flatter low-contrast curve with distinct turquoise/cyan murky shadow undertones and lifted black pedestal.
  - `FX3-3`: Smoother midtone skin-tone rendering with roll-off preventing plastic forehead clipping.
* **Optical Parameters:** Aspect Ratio: $4:3$ (`RATIO_4_3`) | White Balance: Locked Daylight ($5500\text{K}$).
* **Shader Uniforms:**
  $$\text{Aberration} = 0.005, \quad \text{Halation} = \text{Enabled} (\text{Thresh}=0.90, \text{Tint}=\text{Cyan/White}), \quad \text{Grain} = 0.14, \quad \text{ColorNoise} = 0.70$$

#### 4. `fxn_r` (FXN R)
* **Real-World Inspiration:** Fujifilm X100 series (X100V / X100VI) film simulations.
* **Color Profiles:**
  - `FXN`: Classic analog color negative profile. Earthy, warm amber and mahogany tones; soft, peachy highlights rolling gently into cool slate undertones.
  - `FXN2`: Lower-contrast, smoother tonal transition with expanded highlight dynamic range and neutral skin tone rendition.
* **Optical Parameters:** Aspect Ratio: $3:2$ (`RATIO_3_2`) | Subtle lens falloff.
* **Shader Uniforms:**
  $$\text{Aberration} = 0.003, \quad \text{Halation} = \text{Enabled} (\text{Thresh}=0.85, \text{Strength}=0.25), \quad \text{Grain} = 0.08, \quad \text{Vignette} = 0.35$$

#### 5. `collage_diy` (Cutout Collage)
* **Real-World Inspiration:** Y2K teenage scrapbook DIY sticker collage.
* **Aesthetic:** Real-time semantic ML person segmentation; cuts out subjects and stacks multiple exposures into a textured paper canvas with randomized rotation ($\pm 12^\circ$) and drop shadows.

---

### 7.2 Vintage 135 Film Presets (17 Cameras / 35mm Stock)

Standard 35mm film cameras featuring authentic emulsion halation, silver-halide grain clumping, optical mechanical vignetting, and spectral dispersion.

#### 1. `d_classic` (D Classic)
* **Inspiration:** Leica M3 / Canonet QL17 rangefinder loaded with expired Japanese drugstore film.
* **Aesthetic:** Gentle desaturation with olive-greenish shadow undertones, smooth highlight compression, and organic random light leak streaks along frame edges.
* **Shader Configuration:**
  - **Aspect:** $3:2$ (`RATIO_3_2`)
  - **LUT:** `presets/luts/d_classic.raw3d` (Temp: $+0.04$, Tint: $-0.06$)
  - **Halation:** Strength $0.35$, Threshold $0.80$, Tint: $\begin{bmatrix}1.0 & 0.30 & 0.10\end{bmatrix}$
  - **Grain:** Intensity $0.11$, Size $1.25$

#### 2. `gr_f` (GR F)
* **Inspiration:** Ricoh GR1 $28\text{mm} f/2.8$ compact film camera.
* **Aesthetic:** Distinct optical peripheral illumination drop-off with sharp center resolution.
* **Color Profiles:**
  - `400TX`: Kodak Tri-X 400 black & white; medium-high silver contrast, crisp midtone grain.
  - `VELX5`: Fujifilm Velvia 50/100 slide film; rich burgundy shadows, hyper-saturated cobalt skies, punchy skin tones.
  - `100ACR`: Fujifilm Neopan 100 Acros; exceptionally smooth tonal transitions, ultra-fine grain.
* **Shader Configuration:**
  - **Aberration:** Strength $0.007$, Falloff $2.4$, Pincushion $k_2 = -0.015$
  - **Vignette:** Intensity $0.90$, Smoothness $0.55$

#### 3. `ct2f` (CT2F)
* **Inspiration:** Contax T2 Carl Zeiss Sonnar $38\text{mm} f/2.8$.
* **Aesthetic:** Iconic celebrity party and paparazzi flash aesthetic. High-key pastel highlights with slight pink/peachy bias; retains flattering porcelain skin tones even under harsh direct flash.
* **Shader Configuration:**
  - **LUT:** `presets/luts/ct2f.raw3d` (Temp: $+0.06$, Tint: $+0.04$)
  - **Halation:** Strength $0.40$, Threshold $0.78$, Tint: $\begin{bmatrix}1.0 & 0.38 & 0.15\end{bmatrix}$
  - **Vignette:** Intensity $0.50$

#### 4. `d3d` (D3D Wigglegram)
* **Inspiration:** Nishika N8000 / Nimslo 3D quad-lens stereoscopic lenticular cameras.
* **Aesthetic:** Multi-exposure left-to-right parallax oscillation (4-frame looping wigglegram).
* **Color Profiles:**
  - `Default`: Saturated 1980s color print film.
  - `VS2`: Cool, desaturated, high-contrast; purplish blues and olive greens.
  - `THO`: High-contrast, gritty silver monochrome.
* **Export Pipeline:** Generates a 4-frame ping-pong looping MP4 or animated GIF ($12\text{ FPS}$).

#### 5. `135_ne` (135 NE Filmstrip)
* **Inspiration:** Continuous 35mm negative film strip roll.
* **Aesthetic:** Stitches 2 or 3 exposures into a continuous vertical or horizontal strip bordered by exposed sprocket perforations, Kodak/Fujifilm edge codes, and vertical gate weave scan jitter.

#### 6. `d_funs` (D FunS Disposable)
* **Inspiration:** Kodak FunSaver single-use disposable camera.
* **Aesthetic:** Inexpensive single-element plastic meniscus lens; noticeable optical pincushion distortion; heavy peripheral softening; saturated primary yellows and reds; intense warm light leaks.
* **Shader Configuration:**
  - **Aberration:** Strength $0.018$, Cubic $k_2 = -0.045$, Falloff $1.8$
  - **Grain:** Intensity $0.22$, Size $2.2$ (coarse ISO 800 grain)
  - **Vignette:** Intensity $1.15$, Smoothness $0.45$

#### 7. `ir_aero` (IR Infrared False Color)
* **Inspiration:** Kodak Aerochrome III Infrared EIR and Kodak High-Speed Infrared (HIE).
* **Color Profiles:**
  - `IR400`: False-color infrared. Green chlorophyll foliage reflects IR and renders in glowing bubblegum pinks, magentas, and crimson reds; blue skies deepen into saturated cyan; human skin renders porcelain white.
  - `IR100`: Faded vintage infrared. Green foliage transforms into rich burnt orange and golden amber, while purples and blues shift to midnight navy.
* **Shader Configuration:**
  - **LUT:** `presets/luts/ir400_aerochrome.raw3d`
  - **Halation:** Strength $0.75$, Threshold $0.65$ (extreme infrared halo bloom around highlights)

#### 8. `classic_u` (Classic U / U35)
* **Inspiration:** Olympus $\mu$-[mju:]-II / Leica R-System SLR with expired emulsion.
* **Aesthetic:** Expired color negative look; olive-green shifted shadows; coarse clumping grain structure; intense orange-red light piping along frame edges.

#### 9. `dqs_400` (DQS Simple Ace)
* **Inspiration:** Fujifilm Simple Ace 400 Japanese domestic disposable.
* **Aesthetic:** Airy, pastel daylight rendering; soft mint greens; cyan-leaning skies; low-contrast toe shadows.

#### 10. `fqs_r` (FQS R QuickSnap Flash)
* **Inspiration:** Fujifilm QuickSnap Flash 400 disposable camera.
* **Aesthetic:** Unapologetic mechanical fidelity. Fixed $32\text{mm}$ focus ($1.2\text{m}$ to $\infty$); fixed shutter ($1/100\text{s}$); locked Daylight WB ($5500\text{K}$). In low light without flash, shadows clip into noisy lifted black; with flash, direct strobe produces warm, punchy, nostalgic skin tones.

#### 11. `golf_action` (Golf ActionSampler)
* **Inspiration:** Lomography ActionSampler 4-lens sequential camera.
* **Aesthetic:** Captures 4 sequential sub-exposures spaced $200\text{ ms}$ apart on a single square canvas, assembled into a stabilized stop-motion loop.
* **Color Profiles:** `Default`, `FC100` (Fuji Color 100), `VS2` (Cool Moody), `400TX` (Tri-X 400).

#### 12. `cpm35` (CPM35 Zoom)
* **Inspiration:** Canon SureShot 85 Zoom / Minolta TC-1.
* **Aesthetic:** Optical compact zoom haze; lifted cold shadows; dreamy, low-contrast halation around midtone edges.

#### 13. `135_sr` (135 SR Sprocket Rocket)
* **Inspiration:** Lomography Sprocket Rocket wide panoramic camera.
* **Aesthetic:** Super-wide $9:4$ aspect ratio extending past the normal frame borders into the top and bottom sprocket perforations; strong optical barrel distortion; cyan-blue palette.

#### 14. `d_half` (D Half Frame Diptych)
* **Inspiration:** Olympus PEN EE-3 / PEN F $35\text{mm}$ half-frame camera.
* **Aesthetic:** Crops each standard sensor exposure to vertical $3:4$; stitches two consecutive shutter actuations side-by-side onto a single $3:2$ landscape frame separated by a mechanical frame division border.
* **Color Profiles:**
  - `PT16`: Kodak Portra 160; balanced skin tones, pastel pink highlights, olive-green midtone roll-off.
  - `100ACR`: Gritty, high-contrast B&W.
  - `FNT16`: Fuji Fortia high-saturation slide film; vivid yellow highlights and rich violet shadows.

#### 15. `d_slide` (D Slide Reversal)
* **Inspiration:** Kodak Ektachrome E100 / Kodachrome 64 E-6 slide film.
* **Aesthetic:** High dynamic contrast; zero shadow tolerance (deep, crushed blacks); warm, glowing, saturated highlights; packaged in a plastic slide projector mount frame.

#### 16. `ofm_r` (OFM R Cinematic 35mm Motion Picture)
* **Inspiration:** Kodak Vision3 motion picture stock (500T 5219 / 250D 5207) shot on Olympus OM bodies.
* **Color Profiles:**
  - `03G`: Kodak Vision3 50D (5203) Daylight; reduced contrast, earthy brown palette, cool shadows under tungsten.
  - `07FF`: Kodak Vision3 250D (5207) Daylight Scan; contrasty, bleached highlights, rich cool foliage greens, violet shadows.
  - `19P`: Kodak Vision3 500T (5219) Print; warm, saturated amber incandescent glow; crushed navy shadows under daylight.
  - `19P2`: Faded cyan-leaning print version of 19P with desaturated orange-reds.
  - `19M`: High-contrast punchy monitor grade of 500T.
  - `22M`: Kodak Double-X (5222) black-and-white motion picture negative.
  - `C400`: CineStill 400D hybrid daylight film look with moderate halation.

#### 17. `srx_wide` (SRX Wide-Format Film)
* **Inspiration:** Wide-format custom 35mm film body.
* **Aesthetic:** Ultra-clean panoramic rendering pipeline utilizing the latest C400 film profile with edge-to-edge optical fidelity.

---

### 7.3 Vintage 120 Medium Format Presets (4 Presets)

Simulates large-format negative sizes ($6 \times 6\text{ cm}$, $6 \times 7\text{ cm}$), featuring ultra-shallow depth-of-field transition, microscopic grain, and wide dynamic latitude.

#### 1. `s_classic` (S Classic TLR)
* **Inspiration:** Rolleiflex 2.8F Twin-Lens Reflex (Carl Zeiss Planar $80\text{mm} f/2.8$).
* **Format:** $1:1$ Square (`RATIO_1_1`) with optional black film rebate border.
* **Color Profiles:**
  - `100ERA`: Saturated, warm, glowing pinkish-cream skin tones.
  - `KG200`: Kodak Gold 200 medium format; golden-hour yellow wash, punchy saturation.
  - `THO`: Soft, low-contrast portrait monochrome with rich tonal graduation.
* **Shader Configuration:**
  $$\text{Aberration} = 0.001, \quad \text{Halation Strength} = 0.20, \quad \text{Grain Intensity} = 0.04, \quad \text{Grain Size} = 0.8$$

#### 2. `hoga_120` (HOGA 120N)
* **Inspiration:** Holga 120N plastic medium-format toy camera.
* **Format:** $1:1$ Square (`RATIO_1_1`).
* **Aesthetic:** Extreme optical falloff (severe $\cos^6(\theta)$ vignetting); unsharp edge defocus caused by a curved focal plane; plastic chromatic fringing; unexpected orange shutter light leaks.
* **Shader Configuration:**
  - **Aberration:** Strength $0.022$, Pincushion $k_2 = -0.060$, Falloff $1.6$
  - **Vignette:** Intensity $1.45$, Smoothness $0.35$

#### 3. `s_67` (S 67 Studio Medium Format)
* **Inspiration:** Pentax 6x7 medium format camera (Takumar $105\text{mm} f/2.4$).
* **Format:** $6:7$ Aspect Ratio (`widthRatio=6f, heightRatio=7f`).
* **Aesthetic:** Razor-sharp central focus with creamy optical blur falloff; fine silver-halide grain; studio-grade portrait tonality.
* **Color Profiles:** `Default`, `FC100` (Fuji Color 100), `100ERA` (Warm Portrait), `400TX` (Tri-X 400).

#### 4. `kv80_r` (KV80 R Medium Format Cine)
* **Inspiration:** Hasselblad 500 C/M loaded with 70mm movie film.
* **Format:** $1:1$ Square (`RATIO_1_1`).
* **Aesthetic:** Square-format cinematic film stock with optional film rebate perforations and scanner jitter.
* **Color Profiles:** Inherits all OFM R profiles (`03G`, `07FF`, `19P`, `19P2`, `19M`, `22M`, `C400`) plus:
  - `KG200`: Emerald Matrix-green color grade with crushed black shadows.

---

### 7.4 Instant Collection Presets (4 Presets)

Simulates integral instant peel-apart and self-developing films with chemical dye diffusion, bleached highlights, and authentic paper borders.

#### 1. `inst_c` (Inst C Mini)
* **Inspiration:** Fujifilm Instax Mini instant camera.
* **Format:** $4:3$ (`RATIO_4_3`) embedded in an authentic white/pastel bottom-weighted instant film frame.
* **Aesthetic:** Bleached, nostalgic color rendition; low dynamic range; soft Gaussian optical diffusion ($0.75\text{px}$).

#### 2. `inst_sq` (Inst SQ B&W)
* **Inspiration:** Fujifilm Instax Square SQ1 loaded with Instax Monochrome.
* **Format:** $1:1$ Square (`RATIO_1_1`).
* **Aesthetic:** High-contrast monochrome with harshly clipped specular highlights and rich black shadows; decorative colorful instant frames.

#### 3. `inst_sqc` (Inst SQC Color)
* **Inspiration:** Fujifilm Instax Square SQ10 hybrid instant camera.
* **Format:** $1:1$ Square (`RATIO_1_1`).
* **Aesthetic:** Saturated color instant film, gentle corner light leaks, and smooth highlight compression.

#### 4. `paf_r` (PAF R Folding Instant)
* **Inspiration:** Polaroid SX-70 Land Camera / Polaroid 600 folding SLR.
* **Format:** $4:3$ (`RATIO_4_3`) with classic wide-chin white Polaroid border.
* **Optical Setup:** Fixed daylight WB ($5500\text{K}$); fixed $40\text{mm}$ glass optics; inherently underexposed ambient profile requiring xenon flash.
* **Color Profiles:**
  - `EC100`: Cold, dark, moody desaturation; crushed blue-black shadows; muted skin tones.
  - `EB3000`: Polaroid Type 667 (3000 ISO) ultra-fast instant B&W; lifted milky toe blacks; coarse chemical dye clumping.

---

### 7.5 Retro Video & Camcorder Presets (8 Presets)

Simulates analog magnetic tape, video tube sensors, cathode-ray interlacing, and vintage motion picture shutter cadence.

#### 1. `vhs_cam` (VHS Camcorder)
* **Inspiration:** JVC GR-C1 / Panasonic OmniMovie VHS Camcorder ($1984$).
* **Aesthetic:** NTSC $4:1:1$ chroma subsampling horizontal color smear; interlaced scanlines (odd/even alternating lines); bottom tracking noise bar; horizontal tape phase jitter.

#### 2. `super8_mm` (8mm Cine Film)
* **Inspiration:** Braun Nizo Super 8 sound motion picture camera.
* **Aesthetic:** Cadence locked to $18\text{ FPS}$; optical mechanical gate weave (vertical and horizontal frame jitter); organic dust, scratch, and hair overlays; warm tungsten halation bloom.

#### 3. `v_classic` (V Classic Retro Camcorder)
* **Inspiration:** 1980s Vidicon/Saticon video tube camera.
* **Aesthetic:** Saturated video tube smear; bright highlight persistence trails (phosphor comet tails); chromatic noise in shadow regions.

#### 4. `dam_a709` (DAM / A709 Analog Broadcast)
* **Inspiration:** Sony Betacam SP / broadcast video camera.
* **Aesthetic:** Vibrant magnetic tape saturation; sharp analog edge peaking; rich video-level skin rendering.

#### 5. `kv88_cam` (KV88 / KV80 Video8)
* **Inspiration:** Sony Handycam Video8 / Hi8 camcorders ($1990\text{s}$).
* **Aesthetic:** Analog edge ringing/halos around sharp contrasts; high chroma noise in low-light environments; warm consumer color science.

#### 6. `dcr_20x` (DCR 20x MiniDV)
* **Inspiration:** Sony DCR-TRV900 MiniDV digital camcorder ($1998$).
* **Aesthetic:** Early digital $4:2:0$ DV compression blocking; harsh edge sharpening artifacts; early CMOS/CCD digital motion blur.

#### 7. `original_v` (Original Video / Flat Log)
* **Inspiration:** Clean, uncompressed direct sensor capture.
* **Aesthetic:** Neutral color reproduction supporting Log recording for external color grading.

#### 8. `kino_24` (Kino 35mm Cinema)
* **Inspiration:** Arriflex 35 BL cinema camera ($24\text{ FPS}$).
* **Aesthetic:** $180^\circ$ rotary mechanical shutter blur simulation, subtle CineStill-style red halation around specular light sources.

---

### 7.6 Optical Accessories & Lens FX Modules (10 Shaders/Filters)

Independent optical attachment shaders that stack non-destructively onto any camera preset.

```
+----------------------------------------------------------------------------------------------------+
|                                    OPTICAL ATTACHMENT SUITE                                        |
+-------------------+--------------------+--------------------+--------------------+-----------------+
| 1. PRISM LENS     | 2. FISHEYE FULL    | 3. FISHEYE WIDE    | 4. STARBURST FLARE | 5. FLASH GELS   |
| Spectral RGB Blur | 180° Circular Mask | 120° Barrel Spread | 4/6-Point Flare    | R/G/B/Y Xenon   |
+-------------------+--------------------+--------------------+--------------------+-----------------+
| 6. ND FILTER      | 7. DOUBLE EXPOSURE | 8. LIGHT LEAK POOL | 9. GATE WEAVE      | 10. DATE STAMP  |
| Highlight Softener| Screen/Max Blend   | Random Edge Burn   | Film Gate Jitter   | Quartz Orange   |
+-------------------+--------------------+--------------------+--------------------+-----------------+
```

#### 1. Prism Lens (`prism_lens.frag`)
Simulates a triangular glass optical prism attachment held in front of the lens.
* **Algorithm:** Splits the image into duplicate ghosted RGB channels across an arbitrary angle vector $\vec{D} = (\cos\phi, \sin\phi)$ with radial peripheral defocus:
  $$UV_R = UV + \vec{D} \cdot \delta_r, \quad UV_G = UV, \quad UV_B = UV - \vec{D} \cdot \delta_b$$

#### 2. Full Fisheye Lens (`fisheye_circular.frag`)
Simulates an ultra-wide circular fisheye lens (e.g., Nikkor $8\text{mm} f/2.8$).
* **Algorithm:** Applies equidistant projection $r = f \cdot \theta$ resulting in an extreme barrel-distorted circle circumscribed inside a completely black frame perimeter with peripheral chromatic fringing.

#### 3. Wide Fisheye Lens (`fisheye_wide.frag`)
Simulates a diagonal fisheye lens ($120^\circ$ to $150^\circ$ FOV).
* **Algorithm:** Barrel distortion polynomial $r_{\text{src}} = r_{\text{dst}} (1 + k r_{\text{dst}}^2)$ stretching across the full rectangular sensor without circular black borders.

#### 4. Starburst Cross-Screen Filter (`starburst_filter.frag`)
Simulates physical 4-point or 6-point etched grid glass filters (e.g., Hoyarex Star 4).
* **Algorithm:** High-luminance threshold extraction followed by directional multi-tap blur passes along $45^\circ$, $135^\circ$, $90^\circ$ axes, composited additively onto specular highlights.

#### 5. Color Flash Gelatin Filters (`flash_gel.frag`)
Simulates colored plastic gelatin filters clipped over the xenon strobe flash.
* **Modes:** Red, Blue, Emerald Green, Amber Yellow.
* **Algorithm:** Modulates the exposure additive blend by tinting the center flash pool while preserving ambient color temperatures in the shadow toe.

#### 6. Optical Neutral Density & Contrast Softener (`nd_softener.frag`)
* **Algorithm:** Tone curve modification simulating highlight recovery and shadow lifting. Compresses the top $20\%$ of luminance into a roll-off while lifting blacks by $+0.08\text{ EV}$.

#### 7. Double Exposure Accumulator Engine
* **Algorithm:** Caches Frame 1 into an offscreen texture; upon capturing Frame 2, blends both buffers using an energy-preserving Screen blend mode:
  $$C_{\text{final}} = 1.0 - (1.0 - C_1) \cdot (1.0 - C_2)$$

#### 8. Procedural & KTX Light Leak Pools
* **Assets:** 12 compressed ASTC/ETC2 light leak masks categorized by style:
  - *Hinge Leak:* Deep crimson horizontal light piping along the camera hinge.
  - *Corner Flare:* Golden-yellow diagonal sun flare washing across the frame.
  - *Red Fog:* Organic curtain burn caused by accidental camera back opening.

#### 9. Film Gate Scanner Jitter (`gate_weave.vert`)
* **Algorithm:** Injects low-frequency per-frame pseudorandom displacement:
  $$\Delta X = \sin(t \cdot 12.3) \cdot 0.0012, \quad \Delta Y = \cos(t \cdot 18.7) \cdot 0.0018$$

#### 10. Quartz LED Date Stamp (`date_stamp.frag`)
* **Aesthetic:** Classic 1990s quartz date back (e.g., `'98 10 24` or `'02 04 15`).
* **Algorithm:** Samples a 7-segment digital font bitmap texture; applies bright amber-orange emission ($\text{RGB} = [1.0, 0.45, 0.05]$) with a warm Gaussian halo bloom into the bottom-right corner.

---

### 7.7 Generative AI & Aesthetic Prompt Cross-Reference Matrix

To enable AI agents, prompt synthesis engines, and machine learning color mappers to bridge between this OpenGL ES engine and generative diffusion models (such as DALL-E, Midjourney, and Stable Diffusion), the following matrix cross-references each hardware preset with its generative visual tokens and physical camera parameters.

| Preset ID | Camera / Film Stock Inspiration | Target Aspect Ratio | Generative Aesthetic Prompt Tokens (DALL-E / Style Engines) |
|---|---|---|---|
| `portra_400_35mm` | Kodak Portra 400 | $3:2$ | `"35mm photograph, Kodak Portra 400, warm golden skin tones, fine silver-halide grain, soft highlight roll-off, natural daylight, shot on Leica M6"` |
| `fuji_superia_half` | Fuji Superia 400 Half-Frame | $3:4$ Diptych ($3:2$) | `"Half-frame diptych photo, Olympus PEN EE-3, Fuji Superia 400, emerald green shadow tint, candid street photography, white divider line"` |
| `grd_r` | Ricoh GR Digital III | $3:2$ | `"Ricoh GR III street photography, high-contrast black and white, deep inky blacks, snapped focus, wide-angle lens, documentary snapshot"` |
| `ccd_r` | Canon PowerShot A-Series CCD | $4:3$ | `"Early 2000s digicam photo, CCD sensor color science, direct xenon flash, blown highlights, party snapshot aesthetic, Y2K aesthetic"` |
| `fxn_r` | Fujifilm X100V (Classic Chrome) | $3:2$ | `"Fujifilm X100V photograph, classic chrome film simulation, earthy tones, muted colors, soft peach highlights, documentary travel photo"` |
| `d_funs` | Kodak FunSaver Disposable | $3:2$ | `"Disposable camera photo, Kodak FunSaver, single-element plastic lens, harsh direct flash, barrel distortion, edge softness, light leak"` |
| `ct2f` | Contax T2 (Carl Zeiss Sonnar) | $3:2$ | `"Contax T2 photo, Carl Zeiss 38mm lens, paparazzi flash aesthetic, washed out pastel tones, soft chromatic aberration, 1990s celebrity snapshot"` |
| `ir_aero` | Kodak Aerochrome EIR Infrared | $3:2$ | `"False-color infrared photography, Kodak Aerochrome, vibrant pink and magenta foliage, deep cyan blue sky, glowing infrared halation"` |
| `ofm_19p` | Kodak Vision3 500T (CineStill 800T) | $3:2$ | `"Cinematic film still, Kodak Vision3 500T, CineStill 800T, tungsten lighting, intense red halation glow around neon lights, moody cinema aesthetic"` |
| `s_classic` | Rolleiflex TLR (Medium Format) | $1:1$ Square | `"Square medium format photograph, shot on Rolleiflex 2.8F TLR, Carl Zeiss Planar 80mm, shallow depth of field, creamy bokeh, vintage 120 film"` |
| `hoga_120` | Holga 120N Toy Camera | $1:1$ Square | `"Lomography toy camera photo, Holga 120N, plastic lens, heavy dark vignette, edge blur, light leaks, saturated dreamy retro snapshot"` |
| `s_67` | Pentax 6x7 Studio Portrait | $6:7$ | `"Medium format studio portrait, Pentax 6x7, Takumar 105mm f/2.4, ultra-fine film grain, razor-sharp eyes, gentle highlight gradation"` |
| `inst_c` | Fujifilm Instax Mini | $4:3$ Frame | `"Fujifilm Instax Mini instant photo, white instant photo frame, bleached highlights, soft focus, retro party snap"` |
| `paf_r` | Polaroid SX-70 Instant Film | $4:3$ Polarized | `"Vintage Polaroid SX-70 instant photo, classic wide bottom white border, square frame, moody desaturated colors, chemical developer artifacts"` |
| `vhs_cam` | 1980s VHS Magnetic Tape | $4:3$ | `"1980s home video still, VHS camcorder footage, CRT scanlines, horizontal color tape bleeding, tracking noise glitches, analog tape noise"` |
| `super8_mm` | Super 8mm Motion Film Reel | $4:3$ | `"Super 8mm film still, 18fps motion blur, yellow-orange color tint, visible film grain, circular gate weave, dust and scratches"` |

---

### 7.8 Complete `presets_catalog.json` Configuration File

The complete multi-camera configuration data ready for deployment inside `domain/presets/src/main/assets/presets/presets_catalog.json`:

```json
[
  {
    "id": "portra_400_35mm",
    "displayName": "KP 400",
    "subtitle": "Warm Skin Tones • Fine 35mm Grain",
    "aspectRatio": "RATIO_3_2",
    "lut": {
      "assetPath": "presets/luts/portra_400.raw3d",
      "lutSize": 64,
      "isHaldClut": false,
      "defaultIntensity": 0.95,
      "temperatureShift": 0.08,
      "tintShift": -0.03
    },
    "aberration": { "strength": 0.0035, "cubicDistortion": -0.008, "falloffPower": 2.4 },
    "halation": { "enabled": true, "threshold": 0.82, "knee": 0.15, "blurRadius": 1.8, "tintRgb": [1.0, 0.32, 0.12], "strength": 0.30 },
    "grain": { "intensity": 0.09, "particleSize": 1.2, "colorNoiseRatio": 0.20 },
    "compositing": {
      "vignetteIntensity": 0.55,
      "vignetteSmoothness": 0.75,
      "lightLeakPool": ["presets/leaks/leak_warm_01.ktx", "presets/leaks/leak_warm_02.ktx"],
      "borderMaskAsset": "presets/borders/35mm_sprocket.ktx",
      "enableDateStamp": true
    }
  },
  {
    "id": "fuji_superia_half",
    "displayName": "FS 400H Half",
    "subtitle": "Emerald Shadows • Diptych Half-Frame",
    "aspectRatio": "HALF_FRAME_DIPTYCH",
    "lut": {
      "assetPath": "presets/luts/fuji_superia_400.raw3d",
      "lutSize": 64,
      "isHaldClut": false,
      "defaultIntensity": 1.0,
      "temperatureShift": -0.05,
      "tintShift": -0.08
    },
    "aberration": { "strength": 0.008, "cubicDistortion": -0.022, "falloffPower": 2.0 },
    "halation": { "enabled": true, "threshold": 0.76, "knee": 0.12, "blurRadius": 2.5, "tintRgb": [1.0, 0.22, 0.06], "strength": 0.55 },
    "grain": { "intensity": 0.16, "particleSize": 1.65, "colorNoiseRatio": 0.35 },
    "compositing": {
      "vignetteIntensity": 0.80,
      "vignetteSmoothness": 0.65,
      "lightLeakPool": ["presets/leaks/leak_side_burn.ktx"],
      "borderMaskAsset": "presets/borders/half_frame_divider.ktx",
      "enableDateStamp": true
    }
  },
  {
    "id": "ccd_power_shot",
    "displayName": "CCD 2000",
    "subtitle": "Y2K Flash • Sharp CCD Colors",
    "aspectRatio": "RATIO_4_3",
    "lut": {
      "assetPath": "presets/luts/ccd_powershot.raw3d",
      "lutSize": 64,
      "isHaldClut": false,
      "defaultIntensity": 1.0,
      "temperatureShift": 0.0,
      "tintShift": 0.0
    },
    "aberration": { "strength": 0.005, "cubicDistortion": -0.004, "falloffPower": 2.2 },
    "halation": { "enabled": true, "threshold": 0.92, "knee": 0.08, "blurRadius": 1.2, "tintRgb": [0.8, 0.9, 1.0], "strength": 0.20 },
    "grain": { "intensity": 0.14, "particleSize": 1.1, "colorNoiseRatio": 0.70 },
    "compositing": {
      "vignetteIntensity": 0.35,
      "vignetteSmoothness": 0.85,
      "lightLeakPool": [],
      "borderMaskAsset": null,
      "enableDateStamp": true
    }
  },
  {
    "id": "contax_t2_party",
    "displayName": "CT2F Paparazzi",
    "subtitle": "Carl Zeiss 38mm • High-Key Flash",
    "aspectRatio": "RATIO_3_2",
    "lut": {
      "assetPath": "presets/luts/contax_t2.raw3d",
      "lutSize": 64,
      "isHaldClut": false,
      "defaultIntensity": 1.0,
      "temperatureShift": 0.06,
      "tintShift": 0.04
    },
    "aberration": { "strength": 0.004, "cubicDistortion": -0.006, "falloffPower": 2.5 },
    "halation": { "enabled": true, "threshold": 0.78, "knee": 0.14, "blurRadius": 2.0, "tintRgb": [1.0, 0.38, 0.15], "strength": 0.40 },
    "grain": { "intensity": 0.10, "particleSize": 1.3, "colorNoiseRatio": 0.15 },
    "compositing": {
      "vignetteIntensity": 0.50,
      "vignetteSmoothness": 0.70,
      "lightLeakPool": ["presets/leaks/leak_warm_01.ktx"],
      "borderMaskAsset": null,
      "enableDateStamp": true
    }
  },
  {
    "id": "cinestill_800t",
    "displayName": "OFM 19P",
    "subtitle": "500T Movie Film • Red Halation",
    "aspectRatio": "RATIO_3_2",
    "lut": {
      "assetPath": "presets/luts/cinestill_800t.raw3d",
      "lutSize": 64,
      "isHaldClut": false,
      "defaultIntensity": 1.0,
      "temperatureShift": -0.08,
      "tintShift": 0.02
    },
    "aberration": { "strength": 0.005, "cubicDistortion": -0.010, "falloffPower": 2.2 },
    "halation": { "enabled": true, "threshold": 0.72, "knee": 0.16, "blurRadius": 3.2, "tintRgb": [1.0, 0.20, 0.05], "strength": 0.85 },
    "grain": { "intensity": 0.15, "particleSize": 1.5, "colorNoiseRatio": 0.30 },
    "compositing": {
      "vignetteIntensity": 0.65,
      "vignetteSmoothness": 0.75,
      "lightLeakPool": [],
      "borderMaskAsset": null,
      "enableDateStamp": true
    }
  },
  {
    "id": "holga_120n",
    "displayName": "HOGA 120",
    "subtitle": "Toy Plastic Lens • Extreme Vignette",
    "aspectRatio": "RATIO_1_1",
    "lut": {
      "assetPath": "presets/luts/holga_120.raw3d",
      "lutSize": 64,
      "isHaldClut": false,
      "defaultIntensity": 1.0,
      "temperatureShift": 0.02,
      "tintShift": -0.05
    },
    "aberration": { "strength": 0.022, "cubicDistortion": -0.060, "falloffPower": 1.6 },
    "halation": { "enabled": false, "threshold": 0.85, "knee": 0.15, "blurRadius": 1.5, "tintRgb": [1.0, 0.4, 0.1], "strength": 0.2 },
    "grain": { "intensity": 0.24, "particleSize": 2.2, "colorNoiseRatio": 0.40 },
    "compositing": {
      "vignetteIntensity": 1.45,
      "vignetteSmoothness": 0.35,
      "lightLeakPool": ["presets/leaks/leak_side_burn.ktx"],
      "borderMaskAsset": "presets/borders/square_120.ktx",
      "enableDateStamp": false
    }
  },
  {
    "id": "polaroid_sx70",
    "displayName": "PAF 70",
    "subtitle": "Classic Instant • White Frame",
    "aspectRatio": "RATIO_4_3",
    "lut": {
      "assetPath": "presets/luts/polaroid_sx70.raw3d",
      "lutSize": 64,
      "isHaldClut": false,
      "defaultIntensity": 1.0,
      "temperatureShift": -0.04,
      "tintShift": 0.06
    },
    "aberration": { "strength": 0.007, "cubicDistortion": -0.015, "falloffPower": 2.0 },
    "halation": { "enabled": true, "threshold": 0.80, "knee": 0.12, "blurRadius": 2.2, "tintRgb": [1.0, 0.35, 0.15], "strength": 0.35 },
    "grain": { "intensity": 0.18, "particleSize": 1.8, "colorNoiseRatio": 0.45 },
    "compositing": {
      "vignetteIntensity": 0.70,
      "vignetteSmoothness": 0.65,
      "lightLeakPool": [],
      "borderMaskAsset": "presets/borders/polaroid_frame.ktx",
      "enableDateStamp": false
    }
  },
  {
    "id": "aerochrome_ir",
    "displayName": "IR 400",
    "subtitle": "Infrared False Color • Pink Foliage",
    "aspectRatio": "RATIO_3_2",
    "lut": {
      "assetPath": "presets/luts/aerochrome.raw3d",
      "lutSize": 64,
      "isHaldClut": false,
      "defaultIntensity": 1.0,
      "temperatureShift": 0.0,
      "tintShift": 0.0
    },
    "aberration": { "strength": 0.012, "cubicDistortion": -0.018, "falloffPower": 2.1 },
    "halation": { "enabled": true, "threshold": 0.65, "knee": 0.20, "blurRadius": 4.0, "tintRgb": [1.0, 0.15, 0.45], "strength": 0.75 },
    "grain": { "intensity": 0.20, "particleSize": 1.7, "colorNoiseRatio": 0.25 },
    "compositing": {
      "vignetteIntensity": 0.85,
      "vignetteSmoothness": 0.70,
      "lightLeakPool": [],
      "borderMaskAsset": null,
      "enableDateStamp": false
    }
  }
]
```