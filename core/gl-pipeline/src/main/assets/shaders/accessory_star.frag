#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform vec2 uResolution;      // Current FBO dimensions (width, height)
uniform float uStarAngle;      // Star orientation in radians (e.g. 0.7853982 for 45 deg)
uniform float uThreshold;      // Highlight luminance threshold (0.65 - 0.92)
uniform float uStarLength;     // Maximum UV ray reach (0.04 - 0.15)
uniform float uStarIntensity;  // Star flare intensity (0.5 - 2.0)

const int TAP_COUNT = 8;
const vec3 LUMINANCE_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);

vec3 extractHighlight(vec3 rgb) {
    float lum = dot(rgb, LUMINANCE_WEIGHTS);
    if (lum < uThreshold) {
        return vec3(0.0);
    }
    float factor = (lum - uThreshold) / max(1.0 - uThreshold, 0.001);
    // Smooth quadratic response for soft highlight falloff
    return rgb * (factor * factor);
}

void main() {
    vec4 baseColor = texture(uInputTexture, vTexCoord);
    
    // Aspect ratio correction for symmetric star rays
    vec2 aspectScale = vec2(1.0, uResolution.x / uResolution.y);

    // Direction vectors for 4-point cross (two orthogonal axes)
    vec2 dir1 = vec2(cos(uStarAngle), sin(uStarAngle)) * aspectScale;
    vec2 dir2 = vec2(-sin(uStarAngle), cos(uStarAngle)) * aspectScale;

    vec3 starAccum = vec3(0.0);
    float totalWeight = 0.0;

    float stepSize = uStarLength / float(TAP_COUNT);

    // Etched diffraction rays along 4 spokes (+dir1, -dir1, +dir2, -dir2)
    for (int i = 1; i <= TAP_COUNT; i++) {
        float fi = float(i);
        float offset = fi * stepSize;
        float weight = exp(-fi * 0.32);

        // Spectral chromatic dispersion along diffraction rays:
        // Blue/violet diffracts less (inner), red diffracts more (outer)
        vec3 spectralTint = vec3(
            0.8 + 0.4 * (fi / float(TAP_COUNT)),
            0.9,
            1.2 - 0.4 * (fi / float(TAP_COUNT))
        );

        vec2 uv1a = clamp(vTexCoord + dir1 * offset, 0.001, 0.999);
        vec2 uv1b = clamp(vTexCoord - dir1 * offset, 0.001, 0.999);
        vec2 uv2a = clamp(vTexCoord + dir2 * offset, 0.001, 0.999);
        vec2 uv2b = clamp(vTexCoord - dir2 * offset, 0.001, 0.999);

        vec3 h1a = extractHighlight(texture(uInputTexture, uv1a).rgb);
        vec3 h1b = extractHighlight(texture(uInputTexture, uv1b).rgb);
        vec3 h2a = extractHighlight(texture(uInputTexture, uv2a).rgb);
        vec3 h2b = extractHighlight(texture(uInputTexture, uv2b).rgb);

        vec3 sampleSum = (h1a + h1b + h2a + h2b) * spectralTint;
        starAccum += sampleSum * weight;
        totalWeight += weight * 4.0;
    }

    // Normalized and boosted star flare
    vec3 flare = (starAccum / max(totalWeight, 0.001)) * uStarIntensity * 3.5;

    // Optical screen blend with base scene: 1 - (1 - A)*(1 - B)
    vec3 blended = 1.0 - (1.0 - baseColor.rgb) * (1.0 - clamp(flare, 0.0, 1.0));

    fragColor = vec4(blended, baseColor.a);
}
