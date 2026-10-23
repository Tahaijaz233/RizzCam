#!/usr/bin/env python3
"""
Procedural generator for high-fidelity vintage film borders and organic light leaks.
Outputs lightweight, memory-efficient RGBA PNGs:
  - presets/borders/35mm_sprocket.png (35mm film border with sprocket holes)
  - presets/borders/half_frame_divider.png (vertical center divider for diptych)
  - presets/borders/polaroid_frame.png (classic instant frame with wide bottom chin)
  - presets/borders/square_120.png (1:1 square medium format black rebate)
  - presets/leaks/leak_warm_01.png (warm corner sun flare)
  - presets/leaks/leak_side_burn.png (crimson lateral light piping)

VRAM Budget target: Each asset <= 4 MB when loaded uncompressed in RGBA8.
"""

import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_35mm_sprocket(width=1080, height=720):
    """
    35mm film border with sprocket holes along top and bottom edges,
    frame numbers and rebate edge markings, with a transparent center aperture.
    """
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Film rebate margins
    top_margin = 90
    bottom_margin = 90
    left_margin = 35
    right_margin = 35

    # Fill border rebate areas with dark film base
    rebate_color = (14, 14, 14, 255)
    # Top rebate
    draw.rectangle([0, 0, width, top_margin], fill=rebate_color)
    # Bottom rebate
    draw.rectangle([0, height - bottom_margin, width, height], fill=rebate_color)
    # Left rebate
    draw.rectangle([0, 0, left_margin, height], fill=rebate_color)
    # Right rebate
    draw.rectangle([width - right_margin, 0, width, height], fill=rebate_color)

    # Sprocket hole dimensions
    sprocket_w = 42
    sprocket_h = 56
    sprocket_radius = 8
    pitch = 96
    num_sprockets = width // pitch + 1

    # Draw rounded rectangular sprocket holes
    for i in range(num_sprockets):
        cx = i * pitch + 25
        # Top sprocket
        cy_top = top_margin // 2
        top_box = [cx - sprocket_w // 2, cy_top - sprocket_h // 2,
                   cx + sprocket_w // 2, cy_top + sprocket_h // 2]
        # Bottom sprocket
        cy_bottom = height - (bottom_margin // 2)
        bot_box = [cx - sprocket_w // 2, cy_bottom - sprocket_h // 2,
                   cx + sprocket_w // 2, cy_bottom + sprocket_h // 2]

        draw.rounded_rectangle(top_box, radius=sprocket_radius, fill=(0, 0, 0, 0), outline=(28, 28, 28, 255), width=2)
        draw.rounded_rectangle(bot_box, radius=sprocket_radius, fill=(0, 0, 0, 0), outline=(28, 28, 28, 255), width=2)

    # Subtle yellow film stock lettering along bottom margin
    text_color = (200, 180, 80, 220)
    for i in range(0, num_sprockets - 1, 2):
        cx = i * pitch + 60
        draw.text((cx, height - bottom_margin + 6), f"KODAK 400 • {20 + i}A", fill=text_color)
        draw.text((cx, 8), f"▶ {20 + i}", fill=text_color)

    return img

def create_half_frame_divider(width=1080, height=720):
    """
    Half-frame vertical divider bar for 3:4 diptych shots on a 3:2 landscape canvas.
    """
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Center divider bar width ~24px
    divider_w = 26
    cx = width // 2
    x0 = cx - (divider_w // 2)
    x1 = cx + (divider_w // 2)

    bar_color = (12, 12, 12, 255)
    draw.rectangle([x0, 0, x1, height], fill=bar_color)

    # Outer border rim
    border_thick = 8
    draw.rectangle([0, 0, width, border_thick], fill=bar_color)
    draw.rectangle([0, height - border_thick, width, height], fill=bar_color)
    draw.rectangle([0, 0, border_thick, height], fill=bar_color)
    draw.rectangle([width - border_thick, 0, width, height], fill=bar_color)

    # Add subtle vertical hairline scratches/film gate grain
    for y in range(0, height, 4):
        noise_val = int(12 + np.random.randint(-4, 8))
        draw.point((x0 + 1, y), fill=(noise_val, noise_val, noise_val, 255))
        draw.point((x1 - 1, y), fill=(noise_val, noise_val, noise_val, 255))

    return img

def create_polaroid_frame(width=900, height=1080):
    """
    Classic Polaroid instant frame with off-white cardstock and wide bottom chin.
    """
    img = Image.new("RGBA", (width, height), (247, 245, 238, 255))
    draw = ImageDraw.Draw(img)

    # Frame borders
    side_margin = 60
    top_margin = 60
    bottom_chin = 240

    photo_rect = [
        side_margin,
        top_margin,
        width - side_margin,
        height - bottom_chin
    ]

    # Cut out transparent center aperture for the photo
    draw.rectangle(photo_rect, fill=(0, 0, 0, 0))

    # Add realistic inner bezel shadow (1px soft edge)
    draw.rectangle(photo_rect, outline=(210, 205, 195, 220), width=2)

    # Subtle bottom embossed logo area
    logo_color = (200, 196, 186, 180)
    logo_y = height - (bottom_chin // 2) + 20
    draw.text((width // 2 - 40, logo_y), "POLAROID", fill=logo_color)

    return img

def create_square_120(width=1024, height=1024):
    """
    Medium format 120 square mask (6x6) with vintage rounded corners and black border.
    """
    img = Image.new("RGBA", (width, height), (15, 15, 15, 255))
    draw = ImageDraw.Draw(img)

    margin = 48
    corner_radius = 36
    photo_box = [margin, margin, width - margin, height - margin]

    # Draw rounded transparent window
    draw.rounded_rectangle(photo_box, radius=corner_radius, fill=(0, 0, 0, 0))

    # Frame notch markers on left and right edge (classic Hasselblad / Rollei notches)
    notch_w = 6
    notch_h = 24
    draw.rectangle([margin - notch_w, height // 2 - notch_h, margin, height // 2 + notch_h], fill=(0, 0, 0, 0))
    draw.rectangle([width - margin, height // 2 - notch_h, width - margin + notch_w, height // 2 + notch_h], fill=(0, 0, 0, 0))

    # Medium format label
    draw.text((margin + 10, margin - 30), "120 FILM  •  6x6", fill=(120, 120, 120, 200))

    return img

def create_leak_warm_01(width=1024, height=768):
    """
    Warm corner sun flare / optical leak emanating from top-right.
    Smooth radial exponential falloff transitioning from hot white to amber/crimson to black.
    """
    y_coords, x_coords = np.ogrid[:height, :width]
    # Origin at top right
    ox, oy = width * 0.95, height * 0.05
    dx = (x_coords - ox) / width
    dy = (y_coords - oy) / height
    dist = np.sqrt(dx * dx * 1.4 + dy * dy * 0.8)

    # Exponential intensity falloff
    intensity = np.exp(-dist * 4.2)
    # Secondary broad flare
    intensity += 0.35 * np.exp(-dist * 1.8)

    # Color gradient mapping: Core hot white -> Amber -> Deep orange -> Dark red -> 0
    r = np.clip(intensity * 280.0, 0.0, 255.0).astype(np.uint8)
    g = np.clip(np.power(intensity, 1.3) * 190.0, 0.0, 255.0).astype(np.uint8)
    b = np.clip(np.power(intensity, 2.2) * 90.0, 0.0, 255.0).astype(np.uint8)
    a = np.clip(intensity * 255.0, 0.0, 255.0).astype(np.uint8)

    rgba = np.stack([r, g, b, a], axis=-1)
    img = Image.fromarray(rgba, mode="RGBA")
    # Soften with slight Gaussian blur
    img = img.filter(ImageFilter.GaussianBlur(radius=8))
    return img

def create_leak_side_burn(width=1024, height=768):
    """
    Lateral crimson / magenta edge light piping along the left border.
    Simulates light leaking through the camera cassette film door hinge.
    """
    y_coords, x_coords = np.ogrid[:height, :width]
    # Distance from left edge with organic wavy variation
    wave = 0.04 * np.sin(y_coords / 40.0) + 0.02 * np.cos(y_coords / 85.0)
    norm_x = (x_coords / float(width)) - wave

    # Steep falloff from left edge
    intensity = np.exp(-np.maximum(0.0, norm_x) * 7.5)
    # Secondary hot streak near vertical center
    streak = np.exp(-np.abs(y_coords - height * 0.45) / 110.0) * np.exp(-norm_x * 4.0)
    intensity = np.clip(intensity + streak * 0.45, 0.0, 1.0)

    # Crimson to magenta to warm orange palette
    r = np.clip(intensity * 270.0, 0.0, 255.0).astype(np.uint8)
    g = np.clip(np.power(intensity, 2.5) * 85.0, 0.0, 255.0).astype(np.uint8)
    b = np.clip(np.power(intensity, 1.6) * 130.0, 0.0, 255.0).astype(np.uint8)
    a = np.clip(intensity * 255.0, 0.0, 255.0).astype(np.uint8)

    rgba = np.stack([r, g, b, a], axis=-1)
    img = Image.fromarray(rgba, mode="RGBA")
    img = img.filter(ImageFilter.GaussianBlur(radius=6))
    return img

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    workspace_root = os.path.abspath(os.path.join(base_dir, ".."))
    borders_dir = os.path.join(workspace_root, "domain", "presets", "src", "main", "assets", "presets", "borders")
    leaks_dir = os.path.join(workspace_root, "domain", "presets", "src", "main", "assets", "presets", "leaks")

    os.makedirs(borders_dir, exist_ok=True)
    os.makedirs(leaks_dir, exist_ok=True)

    print("Generating Vintage Film Borders and Light Leak Overlay Textures...")
    print("=" * 75)

    overlays = [
        ("borders/35mm_sprocket.png", create_35mm_sprocket(), borders_dir),
        ("borders/half_frame_divider.png", create_half_frame_divider(), borders_dir),
        ("borders/polaroid_frame.png", create_polaroid_frame(), borders_dir),
        ("borders/square_120.png", create_square_120(), borders_dir),
        ("leaks/leak_warm_01.png", create_leak_warm_01(), leaks_dir),
        ("leaks/leak_side_burn.png", create_leak_side_burn(), leaks_dir),
    ]

    for rel_name, img, dest_dir in overlays:
        filename = os.path.basename(rel_name)
        full_path = os.path.join(dest_dir, filename)
        img.save(full_path, format="PNG", optimize=True)
        file_size = os.path.getsize(full_path)
        vram_est = img.width * img.height * 4 / (1024 * 1024)
        print(f"{rel_name:<30} | {img.width}x{img.height:<6} | PNG: {file_size/1024:>6.1f} KB | VRAM: ~{vram_est:.2f} MB")

    print("=" * 75)
    print("All overlay assets successfully generated!")

if __name__ == "__main__":
    main()
