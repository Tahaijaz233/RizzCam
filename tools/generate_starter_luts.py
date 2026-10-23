#!/usr/bin/env python3
"""
Mathematical synthesis of authentic film stock emulations for RizzCam.
Generates 64x64x64 RGB8 binary 3D LUTs (.raw3d) for all 8 starter presets:
  1. portra_400.raw3d: Kodak Portra 400 (warm golden skin tones, soft toe compression, gentle S-curve)
  2. fuji_superia_400.raw3d: Fujifilm Superia 400 (emerald/cyan shadows, magenta midtone pop)
  3. ccd_powershot.raw3d: Early 2000s Digicam (punchy contrast, cyan toe, clipped bright highlights)
  4. contax_t2.raw3d: Contax T2 Carl Zeiss (high-key pastel tones, flattering porcelain highlights)
  5. cinestill_800t.raw3d: CineStill 800T (tungsten movie negative curve, blue-green balance shift)
  6. holga_120.raw3d: Holga 120 (toy lens high-contrast cross-processed saturation)
  7. polaroid_sx70.raw3d: Polaroid SX-70 (chemical dye diffusion, lifted milky black toe, warm peach tint)
  8. aerochrome.raw3d: Kodak Aerochrome (infrared false-color, green foliage -> vibrant pink/crimson)

Each .raw3d file is exactly 64^3 * 3 = 786,432 bytes in C-contiguous order.
"""

import os
import sys
import numpy as np
from PIL import Image

LUT_SIZE = 64
EXPECTED_BYTES = LUT_SIZE * LUT_SIZE * LUT_SIZE * 3  # 786,432 bytes

def create_base_grid(size=LUT_SIZE):
    """
    Creates normalized input coordinate grids [0.0, 1.0].
    b is axis 0 (slowest / depth), g is axis 1 (middle / height), r is axis 2 (fastest / width).
    """
    axis = np.linspace(0.0, 1.0, size, dtype=np.float32)
    b_grid, g_grid, r_grid = np.meshgrid(axis, axis, axis, indexing='ij')
    return r_grid, g_grid, b_grid

def s_curve(x, power=1.5):
    """Smooth S-curve contrast adjustment."""
    # Hermite smoothstep blend with linear
    smooth = x * x * (3.0 - 2.0 * x)
    return np.clip(np.power(smooth, power) if power != 1.0 else smooth, 0.0, 1.0)

def rgb_to_hsv(r, g, b):
    maxc = np.maximum(np.maximum(r, g), b)
    minc = np.minimum(np.minimum(r, g), b)
    v = maxc
    deltac = maxc - minc
    s = np.where(maxc > 1e-6, deltac / (maxc + 1e-6), 0.0)

    rc = np.where(deltac > 1e-6, (maxc - r) / (deltac + 1e-6), 0.0)
    gc = np.where(deltac > 1e-6, (maxc - g) / (deltac + 1e-6), 0.0)
    bc = np.where(deltac > 1e-6, (maxc - b) / (deltac + 1e-6), 0.0)

    h = np.zeros_like(r)
    h = np.where(r == maxc, bc - gc, h)
    h = np.where(g == maxc, 2.0 + rc - bc, h)
    h = np.where(b == maxc, 4.0 + gc - rc, h)
    h = (h / 6.0) % 1.0
    return h, s, v

def hsv_to_rgb(h, s, v):
    i = np.floor(h * 6.0).astype(int)
    f = (h * 6.0) - i
    p = v * (1.0 - s)
    q = v * (1.0 - s * f)
    t = v * (1.0 - s * (1.0 - f))
    i = i % 6

    r = np.zeros_like(h)
    g = np.zeros_like(h)
    b = np.zeros_like(h)

    r = np.where(i == 0, v, np.where(i == 1, q, np.where(i == 2, p, np.where(i == 3, p, np.where(i == 4, t, v)))))
    g = np.where(i == 0, t, np.where(i == 1, v, np.where(i == 2, v, np.where(i == 3, q, np.where(i == 4, p, p)))))
    b = np.where(i == 0, p, np.where(i == 1, p, np.where(i == 2, t, np.where(i == 3, v, np.where(i == 4, v, q)))))
    return r, g, b

def synthesize_portra_400(r, g, b):
    """
    Kodak Portra 400:
    - Gentle S-curve with soft toe and highlight roll-off.
    - Warm golden skin tones: subtle warm shift in midtones (boost R, gentle boost G, soften B).
    - Muted, olive foliage greens.
    - Soft toe compression: blacks lifted to ~0.02.
    """
    luma = 0.2126 * r + 0.7152 * g + 0.0722 * b

    # Soft toe lifting blacks to 0.02, gentle highlight roll-off
    r_curve = 0.02 + 0.98 * (r * 0.2 + 0.8 * (r * r * (3.0 - 2.0 * r)))
    g_curve = 0.015 + 0.985 * (g * 0.25 + 0.75 * (g * g * (3.0 - 2.0 * g)))
    b_curve = 0.01 + 0.99 * (b * 0.35 + 0.65 * (b * b * (3.0 - 2.0 * b)))

    # Warm midtone golden glow
    midtones = np.sin(luma * np.pi)
    r_out = r_curve + 0.05 * midtones
    g_out = g_curve + 0.02 * midtones
    b_out = b_curve - 0.04 * midtones

    # Olive shift in greens
    green_mask = np.maximum(0.0, g - np.maximum(r, b))
    r_out += 0.08 * green_mask
    b_out -= 0.05 * green_mask

    return np.clip(r_out, 0.0, 1.0), np.clip(g_out, 0.0, 1.0), np.clip(b_out, 0.0, 1.0)

def synthesize_fuji_superia_400(r, g, b):
    """
    Fujifilm Superia 400:
    - Emerald / cyan tinted shadows.
    - Magenta midtone pop (4th cyan-sensitive emulsion layer).
    - Cool overall color temperature.
    - Crisp contrast.
    """
    luma = 0.2126 * r + 0.7152 * g + 0.0722 * b

    # Crisp punchy contrast
    r_curve = np.power(r, 1.15) * 1.02
    g_curve = np.power(g, 1.05) * 1.01
    b_curve = np.power(b, 0.95) * 1.04

    # Shadow emerald/cyan tinting (luma < 0.4)
    shadow_mask = np.maximum(0.0, 1.0 - (luma / 0.4))
    r_curve -= 0.04 * shadow_mask
    g_curve += 0.05 * shadow_mask
    b_curve += 0.04 * shadow_mask

    # Midtone magenta pop (0.3 < luma < 0.8)
    mid_mask = np.sin(np.clip(luma, 0.0, 1.0) * np.pi)
    r_out = r_curve + 0.04 * mid_mask
    g_out = g_curve - 0.02 * mid_mask
    b_out = b_curve + 0.03 * mid_mask

    return np.clip(r_out, 0.0, 1.0), np.clip(g_out, 0.0, 1.0), np.clip(b_out, 0.0, 1.0)

def synthesize_ccd_powershot(r, g, b):
    """
    Early 2000s Digicam (PowerShot CCD):
    - Steep punchy contrast, harsh specular clipping.
    - Cyan toe in deep shadows.
    - Punchy primary blues and reds.
    """
    # Linear-steep contrast with sharp clipping above 0.92
    r_out = np.where(r > 0.92, 1.0, np.power(r, 1.25) * 1.1)
    g_out = np.where(g > 0.92, 1.0, np.power(g, 1.20) * 1.08)
    b_out = np.where(b > 0.92, 1.0, np.power(b, 1.15) * 1.12)

    # Cyan toe in shadows
    shadow_mask = np.maximum(0.0, 0.25 - (r + g + b) / 3.0) * 4.0
    r_out -= 0.03 * shadow_mask
    g_out += 0.02 * shadow_mask
    b_out += 0.04 * shadow_mask

    # Saturation boost on primary colors
    h, s, v = rgb_to_hsv(r_out, g_out, b_out)
    s = np.clip(s * 1.22, 0.0, 1.0)
    r_out, g_out, b_out = hsv_to_rgb(h, s, v)

    return np.clip(r_out, 0.0, 1.0), np.clip(g_out, 0.0, 1.0), np.clip(b_out, 0.0, 1.0)

def synthesize_contax_t2(r, g, b):
    """
    Contax T2 (Carl Zeiss Sonnar 38mm):
    - High-key pastel tones, flattering porcelain highlights.
    - Lifted shadows and midtones.
    - Gentle pink/peach skin highlight glow.
    """
    luma = 0.2126 * r + 0.7152 * g + 0.0722 * b

    # High-key exposure lift
    r_curve = np.power(r, 0.90) * 1.02
    g_curve = np.power(g, 0.92) * 1.01
    b_curve = np.power(b, 0.94) * 1.0

    # Pastel desaturation
    h, s, v = rgb_to_hsv(r_curve, g_curve, b_curve)
    s = np.clip(s * 0.92, 0.0, 1.0)
    r_out, g_out, b_out = hsv_to_rgb(h, s, v)

    # Flattering porcelain/pink highlight bloom (luma > 0.6)
    hi_mask = np.maximum(0.0, (luma - 0.6) / 0.4)
    r_out += 0.05 * hi_mask
    g_out += 0.01 * hi_mask
    b_out += 0.03 * hi_mask

    return np.clip(r_out, 0.0, 1.0), np.clip(g_out, 0.0, 1.0), np.clip(b_out, 0.0, 1.0)

def synthesize_cinestill_800t(r, g, b):
    """
    CineStill 800T (Eastman Kodak Vision3 500T):
    - Tungsten white balance negative curve: cool blue-green balance in shadows.
    - Warm incandescent golden highlights.
    - Soft toe, cinematic S-curve.
    """
    luma = 0.2126 * r + 0.7152 * g + 0.0722 * b

    # Cinematic Cineon-style curve
    r_curve = r * r * (3.0 - 2.0 * r)
    g_curve = g * g * (3.0 - 2.0 * g)
    b_curve = b * b * (3.0 - 2.0 * b)

    # Cool teal/blue shadows (tungsten bias)
    shadow_mask = np.maximum(0.0, 1.0 - luma * 1.8)
    r_curve -= 0.05 * shadow_mask
    g_curve += 0.03 * shadow_mask
    b_curve += 0.09 * shadow_mask

    # Warm golden/amber highlights (luma > 0.6)
    hi_mask = np.maximum(0.0, (luma - 0.6) / 0.4)
    r_out = r_curve + 0.08 * hi_mask
    g_out = g_curve + 0.03 * hi_mask
    b_out = b_curve - 0.06 * hi_mask

    return np.clip(r_out, 0.0, 1.0), np.clip(g_out, 0.0, 1.0), np.clip(b_out, 0.0, 1.0)

def synthesize_holga_120(r, g, b):
    """
    Holga 120 (Plastic Toy Lens):
    - Cross-processed high contrast.
    - Saturated yellow-green shadows, deep crushed blacks.
    - Extreme color saturation.
    """
    # Aggressive S-curve
    r_curve = np.power(r * r * (3.0 - 2.0 * r), 1.3) * 1.1
    g_curve = np.power(g * g * (3.0 - 2.0 * g), 1.25) * 1.08
    b_curve = np.power(b * b * (3.0 - 2.0 * b), 1.4) * 1.15

    # Yellow-green cast in shadows
    shadow_mask = np.maximum(0.0, 1.0 - (r + g + b))
    r_curve += 0.04 * shadow_mask
    g_curve += 0.06 * shadow_mask
    b_curve -= 0.08 * shadow_mask

    # Hyper-saturation
    h, s, v = rgb_to_hsv(np.clip(r_curve, 0.0, 1.0), np.clip(g_curve, 0.0, 1.0), np.clip(b_curve, 0.0, 1.0))
    s = np.clip(s * 1.35, 0.0, 1.0)
    r_out, g_out, b_out = hsv_to_rgb(h, s, v)

    return np.clip(r_out, 0.0, 1.0), np.clip(g_out, 0.0, 1.0), np.clip(b_out, 0.0, 1.0)

def synthesize_polaroid_sx70(r, g, b):
    """
    Polaroid SX-70 Instant:
    - Chemical dye diffusion curve.
    - Lifted milky black toe (pedestal at 0.08).
    - Warm peach / amber tint across midtones.
    - Compressed highlight shoulder (max ~0.94).
    """
    luma = 0.2126 * r + 0.7152 * g + 0.0722 * b

    # Lifted milky black toe and compressed shoulder
    r_curve = 0.085 + 0.86 * (r * 0.4 + 0.6 * (r * r * (3.0 - 2.0 * r)))
    g_curve = 0.075 + 0.85 * (g * 0.4 + 0.6 * (g * g * (3.0 - 2.0 * g)))
    b_curve = 0.060 + 0.82 * (b * 0.4 + 0.6 * (b * b * (3.0 - 2.0 * b)))

    # Warm peach dye diffusion tint
    mid_mask = np.sin(np.clip(luma, 0.0, 1.0) * np.pi)
    r_out = r_curve + 0.05 * mid_mask
    g_out = g_curve + 0.02 * mid_mask
    b_out = b_curve - 0.04 * mid_mask

    # Desaturate cold blues slightly
    h, s, v = rgb_to_hsv(r_out, g_out, b_out)
    s = np.clip(s * 0.88, 0.0, 1.0)
    r_out, g_out, b_out = hsv_to_rgb(h, s, v)

    return np.clip(r_out, 0.0, 1.0), np.clip(g_out, 0.0, 1.0), np.clip(b_out, 0.0, 1.0)

def synthesize_aerochrome(r, g, b):
    """
    Kodak Aerochrome Infrared:
    - Chlorophyll / green foliage shifted to vivid pink/crimson/magenta.
    - Blue skies shift to deep cyan/teal.
    - Red/warm objects remain distinct and punchy.
    """
    # Detect vegetation (high Green relative to Blue and Red)
    green_excess = np.maximum(0.0, g - np.maximum(r * 0.8, b * 0.8))
    # General green dominance factor
    veg_factor = np.clip(green_excess * 2.5, 0.0, 1.0)

    # Shift green dominance into intense crimson/magenta/pink
    r_shifted = r * (1.0 - veg_factor) + (g * 1.35 + 0.15) * veg_factor
    g_shifted = g * (1.0 - veg_factor) + (g * 0.25) * veg_factor
    b_shifted = b * (1.0 - veg_factor) + (g * 0.70 + 0.05) * veg_factor

    # Blue sky shifting towards cyan
    sky_factor = np.clip((b - np.maximum(r, g)) * 2.0, 0.0, 1.0)
    g_shifted += 0.15 * sky_factor
    r_shifted -= 0.10 * sky_factor

    # Punchy contrast
    r_out = np.power(np.clip(r_shifted, 0.0, 1.0), 1.1)
    g_out = np.power(np.clip(g_shifted, 0.0, 1.0), 1.15)
    b_out = np.power(np.clip(b_shifted, 0.0, 1.0), 1.05)

    return np.clip(r_out, 0.0, 1.0), np.clip(g_out, 0.0, 1.0), np.clip(b_out, 0.0, 1.0)

def save_lut(lut_tuple, out_raw_path, out_png_path=None):
    """
    Saves (r_out, g_out, b_out) grids into .raw3d and optional HaldCLUT PNG.
    """
    r_out, g_out, b_out = lut_tuple
    # Stack into shape (64, 64, 64, 3) where axis 0=B, 1=G, 2=R
    stacked = np.stack([r_out, g_out, b_out], axis=-1)
    quantized = np.rint(np.clip(stacked, 0.0, 1.0) * 255.0).astype(np.uint8)

    raw_bytes = quantized.tobytes(order='C')
    assert len(raw_bytes) == EXPECTED_BYTES, f"Size mismatch: {len(raw_bytes)} != {EXPECTED_BYTES}"

    os.makedirs(os.path.dirname(os.path.abspath(out_raw_path)), exist_ok=True)
    with open(out_raw_path, 'wb') as f:
        f.write(raw_bytes)

    if out_png_path:
        os.makedirs(os.path.dirname(os.path.abspath(out_png_path)), exist_ok=True)
        # Arrange 64 slices of 64x64 into 8x8 grid (512x512)
        grid = 8
        canvas = np.zeros((LUT_SIZE * grid, LUT_SIZE * grid, 3), dtype=np.uint8)
        for b in range(LUT_SIZE):
            row = b // grid
            col = b % grid
            y0, x0 = row * LUT_SIZE, col * LUT_SIZE
            canvas[y0:y0 + LUT_SIZE, x0:x0 + LUT_SIZE, :] = quantized[b, :, :, :]
        Image.fromarray(canvas, mode='RGB').save(out_png_path, optimize=True)

    return len(raw_bytes)

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    workspace_root = os.path.abspath(os.path.join(base_dir, ".."))
    lut_dest_dir = os.path.join(workspace_root, "domain", "presets", "src", "main", "assets", "presets", "luts")
    hald_dest_dir = os.path.join(lut_dest_dir, "hald")

    print(f"Synthesizing 8 Starter 3D LUTs into: {lut_dest_dir}")
    r, g, b = create_base_grid(LUT_SIZE)

    presets = [
        ("portra_400.raw3d", synthesize_portra_400(r, g, b), "Kodak Portra 400"),
        ("fuji_superia_400.raw3d", synthesize_fuji_superia_400(r, g, b), "Fujifilm Superia 400"),
        ("ccd_powershot.raw3d", synthesize_ccd_powershot(r, g, b), "Canon PowerShot CCD"),
        ("contax_t2.raw3d", synthesize_contax_t2(r, g, b), "Contax T2 Carl Zeiss"),
        ("cinestill_800t.raw3d", synthesize_cinestill_800t(r, g, b), "CineStill 800T"),
        ("holga_120.raw3d", synthesize_holga_120(r, g, b), "Holga 120 Toy Camera"),
        ("polaroid_sx70.raw3d", synthesize_polaroid_sx70(r, g, b), "Polaroid SX-70 Instant"),
        ("aerochrome.raw3d", synthesize_aerochrome(r, g, b), "Kodak Aerochrome Infrared")
    ]

    print("\n" + "=" * 75)
    print(f"{'PRESET FILENAME':<25} | {'EMULATION':<24} | {'BYTE COUNT':<12} | {'STATUS'}")
    print("=" * 75)

    for filename, lut_data, label in presets:
        raw_path = os.path.join(lut_dest_dir, filename)
        hald_name = filename.replace(".raw3d", "_hald.png")
        png_path = os.path.join(hald_dest_dir, hald_name)
        byte_count = save_lut(lut_data, raw_path, png_path)
        status = "EXACT (786,432 B)" if byte_count == EXPECTED_BYTES else "MISMATCH"
        print(f"{filename:<25} | {label:<24} | {byte_count:<12} | {status}")

    print("=" * 75)
    print("All 8 3D LUT binary files and HaldCLUT PNGs successfully generated!\n")

if __name__ == "__main__":
    main()
