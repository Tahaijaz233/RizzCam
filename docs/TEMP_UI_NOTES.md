# RizzCam: Temperature Control UI & Camera Chrome Inventory Notes

## 1. Repo Inventory & Architectural Assessment (§3)

### Inventory Map
| Component | Location (file:line) | Description |
|---|---|---|
| **Dead Thermometer Button** | `feature/viewfinder/.../ui/ViewfinderScreen.kt:600-612` | Floating pill icon with dummy `{ viewModel.haptics.tick() }` callback (line 273). Wired to toggle R5 temperature panel. |
| **Camera Screen** | `feature/viewfinder/.../ui/ViewfinderScreen.kt:109` | Root composable `ViewfinderScreen` rendering viewfinder, framing overlay, control pills, and shutter deck. Hosted in `MainActivity.kt:84`. |
| **CameraX Setup** | `core/camera/.../CameraController.kt:88` | Lifecycle-aware controller managing 4:3 `Preview` and `ImageCapture` use cases. |
| **Preview View** | `feature/viewfinder/.../ui/gl/GlViewfinderSurface.kt:74` | Custom 60 FPS `SurfaceView` backed by `EglCore`, `GlThreadExecutor`, and `GlSurfaceProvider`. |
| **Capture Path** | `feature/viewfinder/.../viewmodel/ViewfinderViewModel.kt:777` | `takePhoto()` calling `executeCaptureAndFilter()` -> `HighResCaptureManager.capturePhoto()` -> `VintageGlPipeline.executeChain()`. |
| **UI Theme / Tokens** | `feature/viewfinder/.../theme/ChromeTokens.kt` | Glassmorphic design tokens (blur, border, fill, typography, spring specs). |
| **State Holder** | `feature/viewfinder/.../viewmodel/ViewfinderViewModel.kt:73` | Holds `StateFlow<ViewfinderState>` (`ViewfinderState.kt`). |
| **Persistence Layer** | `ViewfinderViewModel.kt:115` | `SharedPreferences` (`"rizzcam_prefs"`). Persists per-preset WB state. |

### Environment Details
- **minSdk**: `28` (Android 9 Pie)
- **compileSdk**: `34` (Android 14)
- **CameraX Version**: `1.3.4` (`core/camera/build.gradle.kts:29`)
- **Compose BOM Version**: `2024.06.00` (Kotlin compiler extension `1.5.14`, Kotlin `1.9.24`)

---

## 2. Pipeline Analysis & Architectural Decision (§6.1 vs §6.6)

### Existing Pipeline Architecture
RizzCam already features a high-performance 6-pass OpenGL ES 3.0 graphics engine (`VintageGlPipeline`):
- **Pass 0**: Ingest (`oes_ingest.frag` for live `samplerExternalOES`, `standard_2d_ingest.frag` for still `sampler2D`)
- **Pass 1**: Trilinear 3D LUT (`lut_filter.frag`)
- **Pass 2**: Chromatic Aberration & Lens Distortion (`chromatic_aberration.frag`)
- **Pass 3**: Optical Halation (`halation_threshold.frag`, `separable_blur.frag`, `halation_composite.frag`)
- **Pass 4**: Procedural Simplex Film Grain (`film_grain.frag`)
- **Pass 5**: Optical Compositing & Vignette (`compositing.frag`)

In both preview (`GlViewfinderSurface`) and capture (`HighResCaptureManager`), frames execute through `VintageGlPipeline.executeChain()`.

### Decision on CameraX Effects API vs Custom OpenGL Pipeline
Per spec §3 and §6.6:
1. CameraX version is currently `1.3.4` (where `camera-effects` is not standardized across all streams without breaking custom `GlSurfaceProvider`).
2. RizzCam already owns the complete GL pipeline through `GlSurfaceProvider` and `HighResCaptureManager`.
3. Upgrading Pass 0 with Tanner Helland Kelvin→RGB curve fit + von-Kries luminance normalization + tint shift (§6.3, Appendix A) in `oes_ingest.frag` and `standard_2d_ingest.frag` gives 100% WYSIWYG parity between live preview and captured still JPEG with zero additional latency and zero GC allocations.
4. Hence, following §6.6 fallback / native GL path as sanctioned by §0.3 ("Repo reality wins on compile-correctness, but this spec wins on UX/behavior").

---

## 3. Deviations & Implementation Strategy
1. **Shader Integration**:
   - `oes_ingest.frag` and `standard_2d_ingest.frag` incorporated `kelvinToRgb(uTemperature)` and luminance-preserving von-Kries gain normalization while preserving `uTemperatureShift` and `uTintShift` uniforms for backwards compatibility with `ShaderIntegrityTest`.
2. **State Modeling**:
   - `WhiteBalanceState` added to `ViewfinderState` with `offset`, `tint`, `isAuto`, `offsetDisplay`, `kelvinLabel`.
3. **UI & Glass Chrome**:
   - `ChromeTokens` & `Motion` defined per §5.
   - New `TemperaturePanel` and `TemperatureSlider` components per §7.5 and Appendix B.
   - Interactive glassmorphic pill controls and circular buttons matching the reference screenshot `ref_temp_ui.png`.
4. **Android Build Environment**:
   - Host machine uses zero-host-footprint cloud CI workflow (`.github/workflows/build_and_upload.yml`) with GitHub Actions for release APK generation.
   - Added `gradlew.bat` with automated fallback detection to bundled JDK 21.

---

## 4. Test Matrix & Verification Record (§10 Phase Verification)

### Mathematical & Algorithm Verification
- **Golden Values**:
  - Offset `0` -> `6500K` neutral daylight.
  - Offset `-100` -> `10000K` cool blue sky.
  - Offset `+100` -> `2000K` warm candlelight.
  - Round-trip error < 0.1 mired over [-100..+100].
  - Strictly monotonic Kelvin decay with respect to offset.
  - Tanner Helland RGB curve at 6500K yields `(1.0, 0.997, 0.981) ≈ (1.0, 1.0, 1.0)`.
  - Tanner Helland RGB curve at 3000K yields `(1.0, 0.695, 0.431) ≈ (1.0, 0.70, 0.42)`.
  - Rubber-band touch resistance: linear within [-100..100], 0.35 factor outside.
  - Unicode minus formatting: negative values formatted with `−` (U+2212), e.g. `−9`.

### Shader Pipeline Integrity
- All 13 OpenGL ES 3.0 shaders validated:
  - Header `#version 300 es` as first directive.
  - Float precision declared in all fragment shaders.
  - `uExposureEv`, `uTemperature`, `uTint`, `uStrength` bound in both `oes_ingest.frag` and `standard_2d_ingest.frag`.
  - Balanced brace syntax and entrypoint `void main()` intact.

### Chrome & Gestures
- Sticky press scale physics: `1.0 -> 0.88 -> spring(0.55f, 700f)`.
- Slide panel spring: `spring(dampingRatio = 0.86f, stiffness = 340f)`.
- Double-tap reset on slider track and long-press reset on thermometer circle button.
- Detent haptic feedback at zero crossings and 10-unit intervals.
- Debug intent extras `--es rizzcam.debug.wb_offset` and `--ef rizzcam.debug.wb_offset` wired to `MainActivity`.
