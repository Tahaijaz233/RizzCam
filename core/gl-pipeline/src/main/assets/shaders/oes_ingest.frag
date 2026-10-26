#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform samplerExternalOES uOesTexture;
uniform float uExposureEv;      // Range: [-2.0, +2.0] stops
uniform float uTemperatureShift;// Range: [-1.0, +1.0] (Cool to Warm)
uniform float uTintShift;       // Range: [-1.0, +1.0] (Green to Magenta)
uniform float uTemperature;     // Kelvin (2000..10000), 6500 = neutral
uniform float uTint;            // -100..100, + = magenta
uniform float uStrength;        // 0..1 (0 when auto-WB / bypass)

vec3 kelvinToRgb(float k) {
    float t = k / 100.0;
    float r = (t <= 66.0) ? 1.0 : clamp(1.292936186 * pow(t - 60.0, -0.1332047592), 0.0, 1.0);
    float g = (t <= 66.0) ? clamp(0.39008157876 * log(t) - 0.63184144378, 0.0, 1.0)
                          : clamp(1.129890861 * pow(t - 60.0, -0.0755148492), 0.0, 1.0);
    float b = (t >= 66.0) ? 1.0 : ((t <= 19.0) ? 0.0 : clamp(0.54320678911 * log(t - 10.0) - 1.19625408914, 0.0, 1.0));
    return vec3(r, g, b);
}

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
    
    // 1. Preset white balance modulation
    srgb = applyWhiteBalance(srgb, uTemperatureShift, uTintShift);
    
    // 2. User Kelvin temperature & tint grading (von-Kries luminance-preserving gain)
    if (uStrength > 0.001) {
        vec3 castColor = kelvinToRgb(uTemperature);
        float luma = dot(castColor, vec3(0.2126, 0.7152, 0.0722));
        vec3 gain = (luma > 0.0001) ? (castColor / luma) : vec3(1.0);
        vec3 graded = mix(srgb, srgb * gain, uStrength);
        graded.g  *= 1.0 - (uTint * uStrength) * 0.0025;
        graded.rb *= 1.0 + (uTint * uStrength) * 0.0012;
        srgb = clamp(graded, 0.0, 1.0);
    }
    
    fragColor = vec4(srgb, raw.a);
}
