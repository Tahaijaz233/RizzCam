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
    vec2 tile1 = vec2(mod(slice1, gridRows), floor(slice1 / gridRows)) / gridRows;

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
