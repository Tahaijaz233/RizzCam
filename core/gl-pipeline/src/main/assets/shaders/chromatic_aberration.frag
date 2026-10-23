#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform vec2 uResolution;          // Current FBO dimensions (width, height)
uniform float uAberrationStrength; // Typical range: [0.0, 0.025]
uniform float uCubicDistortion;    // Lens barrel/pincushion k2 coefficient: [-0.05, 0.05]
uniform float uFalloffPower;       // Radial exponent (e.g., 2.0 for quadratic corner buildup)

void main() {
    // Normalize coordinates to aspect-corrected [-1, 1] plane centered at optical axis
    vec2 aspect = vec2(uResolution.x / min(uResolution.x, uResolution.y),
                       uResolution.y / min(uResolution.x, uResolution.y));
    vec2 centeredUv = (vTexCoord - 0.5) * 2.0;
    vec2 opticalCoord = centeredUv * aspect;
    
    float r2 = dot(opticalCoord, opticalCoord);
    float r = sqrt(r2);
    float radialMask = pow(r, uFalloffPower);
    
    // Base radial distortion term: 1 + k1*r^2 + k2*r^4
    float baseDistortion = 1.0 + (uCubicDistortion * r2 * r2);
    
    // Chromatic dispersion offsets per wavelength (Red expands outward, Blue contracts inward)
    float rScale = baseDistortion + (uAberrationStrength * radialMask);
    float gScale = baseDistortion;
    float bScale = baseDistortion - (uAberrationStrength * radialMask * 1.15);
    
    vec2 uvR = clamp(0.5 + (centeredUv * rScale) * 0.5, 0.001, 0.999);
    vec2 uvG = clamp(0.5 + (centeredUv * gScale) * 0.5, 0.001, 0.999);
    vec2 uvB = clamp(0.5 + (centeredUv * bScale) * 0.5, 0.001, 0.999);
    
    // 3-tap sub-stepped dispersion along the radial vector for smooth lateral fringing
    float red   = 0.5 * texture(uInputTexture, uvR).r + 0.5 * texture(uInputTexture, mix(uvG, uvR, 0.5)).r;
    float green = texture(uInputTexture, uvG).g;
    float blue  = 0.5 * texture(uInputTexture, uvB).b + 0.5 * texture(uInputTexture, mix(uvG, uvB, 0.5)).b;
    float alpha = texture(uInputTexture, uvG).a;
    
    fragColor = vec4(red, green, blue, alpha);
}
