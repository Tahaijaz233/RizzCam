#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D u2dTexture;
uniform float uExposureEv;      // Range: [-2.0, +2.0] stops
uniform float uTemperatureShift;// Range: [-1.0, +1.0] (Cool to Warm)
uniform float uTintShift;       // Range: [-1.0, +1.0] (Green to Magenta)

vec3 applyWhiteBalance(vec3 color, float temp, float tint) {
    // CIE XYZ / LMS approximation for fast GPU white balance shift
    vec3 shift = vec3(1.0 + temp * 0.15, 1.0 - tint * 0.10, 1.0 - temp * 0.15);
    return clamp(color * shift, 0.0, 1.0);
}

void main() {
    vec4 raw = texture(u2dTexture, vTexCoord);
    
    // Exposure adjustment in linear-approximated space
    vec3 linearRgb = pow(max(raw.rgb, vec3(0.0)), vec3(2.2));
    linearRgb *= exp2(uExposureEv);
    vec3 srgb = pow(clamp(linearRgb, 0.0, 1.0), vec3(1.0 / 2.2));
    
    // Pre-LUT white balance modulation
    srgb = applyWhiteBalance(srgb, uTemperatureShift, uTintShift);
    
    fragColor = vec4(srgb, raw.a);
}
