# Project: RizzCam — Vintage Camera Architecture & Independent Verification

## Architecture
RizzCam is an ultra-low-latency, skeuomorphic vintage film and digital camera emulation engine for Android. It operates across 5 decoupled stages:
1. **Stage 1 (Core Graphics & Shaders)**: Low-level EGL 1.4 context management with dedicated display-priority thread affinity (`GlThreadExecutor`), a dual full-resolution + dual quarter-resolution half-float (`GL_RGBA16F`) ping-pong FBO pool, asynchronous DMA PBO readback (`PboPhotoReader`), and 13 GLSL ES 3.0 shaders.
2. **Stage 2 (Presets & Assets)**: Domain catalog defining 8 signature camera profiles (`KP 400`, `FS 400H Half`, `CCD 2000`, `CT2F Paparazzi`, `OFM 19P`, `HOGA 120`, `PAF 70`, `IR 400`), 64^3 binary `.raw3d` 3D LUT loader (786,432 bytes, < 1.8 ms upload), 512x512 2D HaldCLUT fallbacks, and ASTC/ETC2 compressed light leak overlays and film border masks.
3. **Stage 3 (Hardware Ingestion & Capture)**: CameraX lifecycle integration with 4:3 native sensor stream locking, zero-copy `GlSurfaceProvider` binding preview frames to `GL_TEXTURE_EXTERNAL_OES` with zero GC allocations, `HighResCaptureManager` offloading full-sensor 6-pass rendering, Olympus PEN half-frame diptych state machine with 14px dark divider, and Android 10-14 Scoped Storage `MediaStoreExporter` with atomic `IS_PENDING` commits.
4. **Stage 4 (Skeuomorphic Viewfinder UI & Haptics)**: Dazz-accurate Jetpack Compose interface featuring an amber quartz LCD HUD, rotary knurled EV thumbwheel (-2.0 EV to +2.0 EV in 1/3-stops), concave mechanical red shutter button with spring travel physics, lens turret wheel, preset vault drawer, hardware volume button shutter intercept, and multi-stage LRA tactile haptics.
5. **Stage 5 (Optical Accessories & Retro Engine)**: Prism, Star, and Fisheye optical glass attachments integrated as Pass 2B; retro video simulation engine locking 18 FPS Super 8 claw flutter, 24 FPS cinema, and 30 FPS VHS tracking jitter; Polaroid SX-70 3.0-second chemical emulsion development with milky cyan opacifier clearing; and 35mm film roll contact sheet gallery on a backlit milk-glass light table with perforated photolab receipt.
6. **Cloud CI/CD & Delivery**: Turnkey GitHub Actions CI (`.github/workflows/build_and_upload.yml`) executing JDK 17 and Gradle compilation strictly on cloud runners (`ubuntu-latest`) with zero host laptop execution footprint and memory bounded under 1536 MB heap.

```
[CameraX Sensor (4:3)] ──(zero-copy)──> [GlSurfaceProvider (OES Texture)]
                                                  │
                                                  ▼
                                       [PingPongFboPool (RGBA16F)]
                                         ├─ Pass 0: OES Ingest (EV + WB)
                                         ├─ Pass 1: 3D LUT (64^3) / HaldCLUT
                                         ├─ Pass 2: Chromatic Aberration
                                         ├─ Pass 2B: Optical Accessories (Prism / Star / Fisheye)
                                         ├─ Pass 3: Halation (1/4 FBO + 9-tap Blur + Screen Composite)
                                         ├─ Pass 4: McEwan Simplex Film Grain (Luma-peaked)
                                         └─ Pass 5: Optical Compositing (Vignette + Leaks + Borders)
                                                  │
                                                  ├─ (Preview: 60 FPS) ──> [GlViewfinderSurface (Screen)]
                                                  └─ (Capture: DMA PBO) ──> [MediaStoreExporter (Scoped Storage)]
```

---

## Feature Inventory

| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | EGL 1.4 & Dedicated Render Thread | Single-threaded HandlerThread with THREAD_PRIORITY_DISPLAY, EGL 1.4 context creation, ES 3.0 client version, and deterministic surface/context destruction | M1 | Survey |
| 2 | Ping-Pong Half-Float FBO Pool | Ping-pong allocator with dual full-res and dual quarter-res RGBA16F FBO attachments (Preview: 35.29 MB <= 35.3 MB; Capture: 204.0 MB <= 204 MB) | M1 | Survey |
| 3 | Zero-Stutter Aspect Ratio Cropping | 4x4 UV transformation matrix pre-multiplying SurfaceTexture transform to switch 4:3, 3:2, 1:1, 3:4 without reallocating camera sessions | M1 | Survey |
| 4 | OES Ingestion Shader | Pass 0 shader converting samplerExternalOES YUV stream into linear sRGB texture storage with pre-LUT EV and CIE XYZ/LMS WB shifts | M1 | Survey |
| 5 | Standard 2D Ingestion Shader | Offline still-capture Pass 0 shader converting high-res 2D texture with pre-LUT EV and WB adjustments | M1 | Survey |
| 6 | Trilinear 3D LUT Color Grading | Pass 1 color remapping using hardware sampler3D (64x64x64) with half-texel center alignment | M1 | Survey |
| 7 | 2D HaldCLUT Fallback Grading | Pass 1 color remapping using 512x512 2D HaldCLUT (8x8 grid of 64x64 slices) with manual Z-slice lerp | M1 | Survey |
| 8 | Radial Chromatic Aberration & Dispersion | Pass 2 Brown-Conrady polynomial radial distortion (1 + k2 r^4) with differential RGB dispersion and 3-tap clamping | M1 | Survey |
| 9 | Optical Halation Sub-Pipeline | Pass 3 highlight thresholding at 1/4 resolution, separable 9-tap Gaussian blur (H & V), warm CineStill red-orange screen composite | M1 | Survey |
| 10 | Procedural Simplex Film Grain | Pass 4 silver-halide clumping using Ian McEwan 2D Simplex noise with midtone-peaked bell response and Pegtop soft-light blend | M1 | Survey |
| 11 | Final Optical Compositing | Pass 5 physical cos^4(theta) lens vignette, randomized screen-blended light leaks, alpha-masked film borders | M1 | Survey |
| 12 | Async DMA PBO Readback | Asynchronous high-res pixel readback using GL_PIXEL_PACK_BUFFER with glFenceSync DMA and orientation matrix flip | M1 | Survey |
| 13 | Fullscreen Quad Vertex Shader | Minimal NDC quad vertex shader with UV passthrough and 4x4 matrix transformation | M1 | Survey |
| 14 | Binary .raw3d 3D LUT Engine | Direct memory-mapped upload of 64^3 RGB8 uncompressed LUT binary (786,432 bytes) in < 1.8 ms | M2 | Survey |
| 15 | Camera Presets Catalog | JSON-driven catalog of 8 deployed analog/digital presets with complete optical and chemical configs | M2 | Survey |
| 16 | Compressed GPU Overlay Assets | ASTC/ETC2 compressed light leak overlays and film border masks (sprocket, 120 square, polaroid, half-frame) | M2 | Survey |
| 17 | Texture Cache Single-Slot Swapping | Single-slot texture replacement with immediate bitmap recycling, bounding preset texture VRAM < 15 MB | M2 | Survey |
| 18 | Zero-Copy GlSurfaceProvider | Bridges CameraX Preview UseCase directly to external OES texture on GL thread with zero per-frame allocations | M3 | Survey |
| 19 | High-Res Capture Manager | Offscreen full-sensor still capture, EXIF extraction, sensor rotation, offscreen FBO processing, PBO DMA readback | M3 | Survey |
| 20 | Olympus PEN Diptych Stitcher | Two-shot half-frame exposure state machine stitching two 3:4 portrait shots with 14px dark divider (#0D0D0D) into 3:2 landscape | M3 | Survey |
| 21 | Scoped Storage MediaStore Exporter | Atomically writes photos to Pictures/RizzCam using IS_PENDING = 1 (API 29+) or public directory (API 28), injects EXIF tags | M3 | Survey |
| 22 | EXIF Telemetry Preservation | Extracts hardware telemetry (ISO, shutter, aperture, focal length) and injects film stock / preset descriptions | M3 | Survey |
| 23 | Amber Digital Quartz LCD HUD | Skeuomorphic top-deck LCD panel with amber backlight, digital segment matrix, EV compensation, frame counter, strobe status | M4 | Survey |
| 24 | Knurled Exposure Thumbwheel | Tactile rotary metal cylinder with vertical micro-grooves, 13 click stops (-2.0 EV to +2.0 EV in 1/3 stops), magnetic spring release | M4 | Survey |
| 25 | Concave Mechanical Shutter Button | Concentric titanium/chrome collar, high-gloss concave red button face, cable release socket, physical spring depression physics | M4 | Survey |
| 26 | Prime Lens Selector Wheel | Rotary titanium pill housing with circular buttons (Fisheye, 28mm, 35mm, 50mm) mapping to CameraX optical/digital zoom | M4 | Survey |
| 27 | Camera Preset Vault Drawer | Modal bottom sheet displaying 2-column grid of camera models with accent pips, aspect ratio badges, and film specifications | M4 | Survey |
| 28 | Hardware Volume Key Intercept | Maps physical volume up/down buttons to trigger the mechanical shutter release | M4 | Survey |
| 29 | Tactile Mechanical Haptics Engine | Linear resonant actuator (LRA) choreography synthesizing dial ticks, shutter clunks, ratchet advance, and camera locks | M4 | Survey |
| 30 | Prism Glass Attachment Shader | Simulates 3-facet or 6-facet cut triangular glass optical prism with radial peripheral deflection, dispersion, and facet bevel reflection | M5 | Survey |
| 31 | Starburst Cross Diffraction Shader | Simulates 4-point etched grid glass filter with directional highlight rays, spectral dispersion, and optical screen blend | M5 | Survey |
| 32 | Fisheye Lens Attachment Shader | Full equidistant circular barrel distortion (1 + k r^2 + 0.35 k^2 r^4) with chromatic edge fringing and circular housing vignette | M5 | Survey |
| 33 | Retro Video Simulation Engine | CameraX VideoCapture / Recorder with real-time film gate flutter, mechanical claw play, scanline jitter, and exposure flicker | M5 | Survey |
| 34 | 18 FPS Super 8 Emulation | Cadence locked to 18 FPS (55ms frame duration), mechanical claw jumps, gate weave, and +/- 4% rotary exposure flicker | M5 | Survey |
| 35 | 24 FPS Cinema 35mm Emulation | Cadence locked to 24 FPS (41ms frame duration), pin-registered gate weave, and subtle rotary shutter exposure variation | M5 | Survey |
| 36 | 30 FPS VHS Tape Emulation | Cadence locked to 30 FPS (33ms frame duration), NTSC helical scan head-switching tracking bursts, and scanline flutter | M5 | Survey |
| 37 | Polaroid SX-70 Development Simulation | Skeuomorphic SX-70 ejection slide-up (650ms), 3.0s chemical development simulation (milky cyan opacifier clearing), flick-to-dismiss physics | M5 | Survey |
| 38 | Backlit Light Table Contact Sheet | Vintage photographer's milk-glass light table with radial backlight, continuous 35mm film strips, sprockets, and amber rebate codes | M5 | Survey |
| 39 | Photolab Development Slip | Perforated thermal receipt modal sheet displaying complete shooting EXIF telemetry (film stock, chemistry, ISO, shutter, aperture) and QC stamp | M5 | Survey |
| 40 | Pinch-to-Zoom Fullscreen Inspector | Interactive gesture-based inspection supporting 1.0x to 5.0x pinch zoom and pan with double-tap reset | M5 | Survey |
| 41 | Zero-Host Cloud Compilation CI | GitHub Actions workflow offloading JDK 17, Gradle wrapper validation, and APK compilation to hosted cloud VMs (.github/workflows/build_and_upload.yml) | M6 | Survey |
| 42 | Turnkey Artifact & Drive Delivery | Packages compiled APK into downloadable GitHub Actions Artifacts (14-day retention) and optional Google Drive export | M6 | Survey |
| 43 | Final Multi-Stage Verification & Certificates | Comprehensive independent verification certificates issued across all 5 stages and CI/CD acceptance criteria | M7 | Survey |

---

## Milestones

| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Stage 1 Core Graphics Audit & Verification | EGL 1.4 context, GlThreadExecutor, PingPongFboPool (Preview <= 35.3 MB, Capture <= 204 MB), PBO DMA Readback, and all 13 GLSL ES 3.0 shaders | none | DONE |
| M2 | Stage 2 Camera Presets & Assets Audit | 8 signature camera profiles, binary 64^3 .raw3d LUT loader, HaldCLUT fallbacks, light leaks, film border masks, texture cache | M1 | DONE |
| M3 | Stage 3 CameraX & Capture Pipeline Audit | Zero-copy GlSurfaceProvider, HighResCaptureManager, Olympus PEN 3:4 diptych stitcher, Scoped Storage MediaStoreExporter | M1, M2 | DONE |
| M4 | Stage 4 Skeuomorphic Compose UI Audit | Amber quartz LCD HUD, knurled EV thumbwheel (-2 to +2 EV), concave mechanical red shutter, lens turret, LRA tactile haptics | M1, M3 | DONE |
| M5 | Stage 5 Optical Accessories & Retro Engine Audit | Prism/Star/Fisheye shaders (Pass 2B), retro video engine (18/24/30 FPS), Polaroid SX-70 chemical dev, 35mm contact sheet light table | M1, M2, M4 | DONE |
| M6 | Turnkey Cloud CI/CD & Delivery Audit | `.github/workflows/build_and_upload.yml`, zero host build processes, JDK 17, Gradle caching, artifact upload, heap <= 1536 MB | none | DONE |
| M7 | Multi-Stage Verification Certificates | Issuance of formal, independent verification certificates for Stages 1–5 and CI/CD, final acceptance sign-off | M1-M6 | DONE |

---

## Interface Contracts

### 1. `core:camera` <-> `core:gl-pipeline`
- **Surface Texture Streaming**:
  - `GlSurfaceProvider`: Binds CameraX `SurfaceRequest` to `SurfaceTexture` attached to `oesTextureId`.
  - Dispatches `onFrameReady(transformMatrix: FloatArray)` on `GlThreadExecutor`.
- **FBO Pool & Offscreen Render**:
  - `PingPongFboPool`: Initialized with `(width, height, useFloat16 = true)`.
  - `VintageGlPipeline.executeChain`: Processes 6-pass pipeline (`isExternalOes = false` for still capture).
  - `PboPhotoReader.readPhotoAsync`: Consumes target `fboId`, executes asynchronous DMA readback using `glFenceSync`, maps buffer to Android `ARGB_8888` Bitmap.

### 2. `domain:presets` <-> `core:gl-pipeline`
- **Preset Model**:
  - `CameraPreset`: Supplies `lutAssetPath`, `grainIntensity`, `halationStrength`, `vignetteIntensity`, `lightLeakAssetPath`, `borderAssetPath`.
- **Texture Cache**:
  - `Lut3dTextureLoader`: Loads binary `.raw3d` ($64 \times 64 \times 64 \times 3$ bytes) via `ByteBuffer` and uploads to `GL_TEXTURE_3D` (`GL_RGB8`, `GL_LINEAR`, `GL_CLAMP_TO_EDGE`).
  - `TextureCacheManager`: Caches border, leak, and LUT textures using single-slot replacement and immediate `bitmap.recycle()`.

### 3. `core:camera` <-> `feature:viewfinder`
- **Capture & Export Dispatch**:
  - `HighResCaptureManager.captureHighResPhoto`: Returns `CapturedPhotoResult(bitmap, exif)`.
  - `HalfFrameDiptychManager`: State machine managing `Empty` -> `FirstFrameReady` (stores 160px preview thumbnail) -> `CompositeReady(stitchedBitmap)`.
  - `MediaStoreExporter.exportPhoto`: Receives processed Bitmap, writes to `Pictures/RizzCam` via `IS_PENDING = 1`, and updates to `IS_PENDING = 0`.

### 4. `feature:viewfinder` <-> UI & Audio/Haptics
- **Tactile Feedback**:
  - `RetroHapticsEngine`: API 31+ `VibratorManager` with fallback to `Vibrator`. Exposes `tick()` (EV dial), `shutterClick()` (two-stage clunk), `diptychAdvance()` (ratchet), and `cameraSwitch()`.
- **Hardware Keys**:
  - `MainActivity`: Intercepts `KEYCODE_VOLUME_DOWN` and `KEYCODE_VOLUME_UP` to actuate shutter release.

---

## Code Layout

- `core/gl-pipeline/`:
  - `src/main/assets/shaders/`: 13 GLSL ES 3.0 shaders (`fullscreen_quad.vert`, 12 fragment shaders).
  - `src/main/java/com/vintagecam/core/gl/egl/`: `EglCore.kt`, `GlThreadExecutor.kt`.
  - `src/main/java/com/vintagecam/core/gl/fbo/`: `FramebufferObject.kt`, `PingPongFboPool.kt`.
  - `src/main/java/com/vintagecam/core/gl/texture/`: `Lut3dTextureLoader.kt`, `TextureCacheManager.kt`.
  - `src/main/java/com/vintagecam/core/gl/pass/`: Pass implementations (`OesIngestPass.kt`, `LutFilterPass.kt`, `AccessoryPass.kt`, etc.).
  - `src/main/java/com/vintagecam/core/gl/`: `VintageGlPipeline.kt`, `PboPhotoReader.kt`.
- `core/camera/`:
  - `src/main/java/com/vintagecam/core/camera/`: `CameraController.kt`, `GlSurfaceProvider.kt`.
  - `src/main/java/com/vintagecam/core/camera/capture/`: `HighResCaptureManager.kt`.
  - `src/main/java/com/vintagecam/core/camera/diptych/`: `HalfFrameDiptychManager.kt`.
  - `src/main/java/com/vintagecam/core/camera/export/`: `MediaStoreExporter.kt`.
  - `src/main/java/com/vintagecam/core/camera/exif/`: `ExifMetadataWriter.kt`.
  - `src/main/java/com/vintagecam/core/camera/video/`: `RetroVideoRecorder.kt`.
- `domain/presets/`:
  - `src/main/assets/presets/`: `presets_catalog.json`.
  - `src/main/assets/presets/luts/`: 8 `.raw3d` 3D LUT binaries (786,432 bytes each).
  - `src/main/assets/presets/luts/hald/`: 8 512x512 HaldCLUT fallback PNGs.
  - `src/main/assets/presets/borders/`: 4 border masks (`35mm_sprocket.png`, `half_frame_divider.png`, `square_120.png`, `polaroid_frame.png`).
  - `src/main/assets/presets/leaks/`: 2 organic light leak overlays.
  - `src/main/java/com/vintagecam/domain/presets/model/`: `CameraPreset.kt`, `PresetCategory.kt`.
- `feature/viewfinder/`:
  - `src/main/java/com/vintagecam/feature/viewfinder/ui/`: `ViewfinderScreen.kt`, `GlViewfinderSurface.kt`.
  - `src/main/java/com/vintagecam/feature/viewfinder/ui/components/`: `RetroLcdHeader.kt`, `KnurledExposureDial.kt`, `SkeuomorphicShutterButton.kt`, `LensSelectorWheel.kt`, `CameraPresetDrawer.kt`.
  - `src/main/java/com/vintagecam/feature/viewfinder/haptics/`: `RetroHapticsEngine.kt`.
  - `src/main/java/com/vintagecam/feature/viewfinder/instant/`: `InstantPhotoOverlay.kt`.
  - `src/main/java/com/vintagecam/feature/viewfinder/gallery/`: `FilmRollGalleryScreen.kt`.
- `app/`:
  - `src/main/java/com/vintagecam/rizzcam/`: `MainActivity.kt`, `RizzCamApplication.kt`.
- `.github/workflows/`:
  - `build_and_upload.yml`: Turnkey zero-host-footprint cloud APK compilation and artifact delivery workflow.
