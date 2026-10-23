#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform vec2 uResolution;              // Current FBO dimensions (width, height)
uniform float uFisheyeCurvature;       // Barrel curvature coefficient (e.g. 0.45 - 0.85)
uniform float uCircularVignetteRadius; // Outer circle cut radius (e.g. 0.88 - 1.05)
uniform float uVignetteSoftness;       // Feather radius of the circular barrel mask (e.g. 0.03 - 0.08)
uniform float uChromaticFringe;        // Lateral color separation at edge (e.g. 0.006 - 0.015)

void main() {
    // Aspect-ratio correction to ensure circular distortion
    vec2 aspect = vec2(uResolution.x / min(uResolution.x, uResolution.y),
                       uResolution.y / min(uResolution.x, uResolution.y));
    vec2 centeredUv = (vTexCoord - 0.5) * 2.0;
    vec2 opticalCoord = centeredUv * aspect;

    float r = length(opticalCoord);
    float r2 = r * r;

    // Polynomial barrel distortion mapping
    // Samples pixels further outward in the source image to compress them into a curved sphere
    float distortion = 1.0 + (uFisheyeCurvature * r2) + (0.35 * uFisheyeCurvature * uFisheyeCurvature * r2 * r2);
    vec2 distortedOptical = opticalCoord * distortion;
    vec2 distortedCentered = distortedOptical / aspect;

    // Wavelength dispersion offsets at extreme optical curvature
    float dispersion = uChromaticFringe * r2;
    vec2 centeredR = distortedCentered * (1.0 + dispersion);
    vec2 centeredG = distortedCentered;
    vec2 centeredB = distortedCentered * (1.0 - dispersion);

    vec2 uvR = 0.5 + centeredR * 0.5;
    vec2 uvG = 0.5 + centeredG * 0.5;
    vec2 uvB = 0.5 + centeredB * 0.5;

    // Check bounds of distorted sample
    bool inBounds = uvG.x >= 0.0 && uvG.x <= 1.0 && uvG.y >= 0.0 && uvG.y <= 1.0;
    
    vec3 sampledColor = vec3(0.0);
    float alpha = 1.0;

    if (inBounds) {
        float rCol = texture(uInputTexture, clamp(uvR, 0.001, 0.999)).r;
        float gCol = texture(uInputTexture, clamp(uvG, 0.001, 0.999)).g;
        float bCol = texture(uInputTexture, clamp(uvB, 0.001, 0.999)).b;
        alpha = texture(uInputTexture, clamp(uvG, 0.001, 0.999)).a;
        sampledColor = vec3(rCol, gCol, bCol);
    }

    // Circular lens barrel housing vignette with subtle metallic inner ring reflection
    float barrelMask = 1.0 - smoothstep(uCircularVignetteRadius - uVignetteSoftness, uCircularVignetteRadius, r);
    
    // Subtle internal lens barrel rim reflection
    float bezelRim = smoothstep(uCircularVignetteRadius - uVignetteSoftness * 1.5, uCircularVignetteRadius - uVignetteSoftness * 0.5, r) *
                     (1.0 - smoothstep(uCircularVignetteRadius - uVignetteSoftness * 0.5, uCircularVignetteRadius, r));
    vec3 rimGlow = vec3(0.04, 0.05, 0.07) * bezelRim;

    vec3 finalColor = (sampledColor * barrelMask) + rimGlow;

    fragColor = vec4(finalColor, alpha);
}
