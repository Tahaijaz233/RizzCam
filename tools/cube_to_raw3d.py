#!/usr/bin/env python3
"""
Converts an Adobe/Resolve .cube 3D LUT file into:
1. A compact binary .raw3d (64x64x64 RGB8) buffer for direct glTexImage3D upload.
2. A 512x512 2D HaldCLUT PNG fallback image.

Implements Section 5.1 of the Clean-Room Architectural Specification.
"""
import sys
import os
import struct
import numpy as np
from PIL import Image

def parse_cube(filepath: str):
    """
    Parses a DaVinci Resolve / Adobe .cube 3D LUT file.
    Returns (size, 3D float array normalized to [0.0, 1.0]).
    """
    size = 0
    data = []
    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#') or line.startswith('TITLE') or line.startswith('DOMAIN'):
                continue
            if line.startswith('LUT_3D_SIZE'):
                size = int(line.split()[1])
                continue
            parts = line.split()
            if len(parts) == 3:
                try:
                    r, g, b = float(parts[0]), float(parts[1]), float(parts[2])
                    data.append((r, g, b))
                except ValueError:
                    continue

    if size == 0:
        # Calculate size assuming cube has equal N x N x N samples
        cube_root = round(len(data) ** (1.0 / 3.0))
        if cube_root ** 3 == len(data):
            size = cube_root
        else:
            raise ValueError(f"Unable to determine 3D LUT size from {filepath} (found {len(data)} entries)")

    lut = np.array(data, dtype=np.float32).reshape((size, size, size, 3))
    return size, np.clip(lut, 0.0, 1.0)

def export_raw3d(lut: np.ndarray, output_path: str):
    """
    Quantizes [0.0, 1.0] float to 8-bit unsigned integers [0, 255] and writes
    raw bytes in C-order (R varies fastest, then G, then B) for OpenGL ES 3.0 glTexImage3D.
    """
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    quantized = np.rint(lut * 255.0).astype(np.uint8)
    raw_bytes = quantized.tobytes(order='C')
    with open(output_path, 'wb') as f:
        f.write(raw_bytes)
    print(f"Exported binary 3D LUT ({len(raw_bytes)} bytes) -> {output_path}")

def export_hald_png(lut: np.ndarray, size: int, output_path: str):
    """
    Arranges 64 slices of 64x64 into an 8x8 2D grid (512x512) for fallback 2D HaldCLUT texture sampling.
    """
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
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
    if len(sys.argv) < 3:
        print("Usage: python cube_to_raw3d.py <input.cube> <output.raw3d> [output_hald.png]")
        sys.exit(1)

    cube_file = sys.argv[1]
    raw_out = sys.argv[2]
    png_out = sys.argv[3] if len(sys.argv) > 3 else None

    lut_size, lut_array = parse_cube(cube_file)
    export_raw3d(lut_array, raw_out)
    if png_out:
        export_hald_png(lut_array, lut_size, png_out)
