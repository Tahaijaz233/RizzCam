#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform vec2 uResolution;          // Current FBO dimensions (width, height)
uniform float uFacetCount;         // 3.0 or 6.0 facets
uniform float uPrismRotation;      // Optical rotation angle in radians
uniform float uRefractionStrength; // Magnitude of facet displacement (e.g., 0.06 - 0.18)
uniform float uChromaticSpread;    // Wavelength dispersion delta (e.g., 0.02 - 0.04)
uniform float uCenterSplit;        // Radius of central un-deflected core (e.g., 0.10)

const float PI = 3.14159265358979323846;

void main() {
    // Aspect-ratio correction to optical coordinates centered at (0, 0)
    vec2 aspect = vec2(uResolution.x / min(uResolution.x, uResolution.y),
                       uResolution.y / min(uResolution.x, uResolution.y));
    vec2 centeredUv = (vTexCoord - 0.5) * 2.0;
    vec2 opticalCoord = centeredUv * aspect;

    float r = length(opticalCoord);
    float angle = atan(opticalCoord.y, opticalCoord.x) + uPrismRotation;

    // Normalize angle to [0, 2*PI)
    angle = mod(angle, 2.0 * PI);
    if (angle < 0.0) angle += 2.0 * PI;

    float facetAngle = (2.0 * PI) / max(uFacetCount, 1.0);
    float facetIndex = floor(angle / facetAngle);
    float sectorAngle = mod(angle, facetAngle);

    // Compute facet center direction vector
    float midFacetAngle = (facetIndex + 0.5) * facetAngle - uPrismRotation;
    vec2 facetDir = vec2(cos(midFacetAngle), sin(midFacetAngle));

    // Facet displacement increases with distance from center
    float radialFalloff = smoothstep(uCenterSplit, uCenterSplit + 0.6, r);
    float shiftMagnitude = uRefractionStrength * radialFalloff;

    // Glass cut line / boundary between adjacent facets
    float edgeDistance = min(sectorAngle, facetAngle - sectorAngle) / (facetAngle * 0.5);
    float facetEdgeLine = smoothstep(0.0, 0.08, edgeDistance);

    // Chromatic dispersion offsets: Red refracted less, Blue refracted more
    vec2 rOffset = facetDir * (shiftMagnitude * (1.0 - uChromaticSpread));
    vec2 gOffset = facetDir * shiftMagnitude;
    vec2 bOffset = facetDir * (shiftMagnitude * (1.0 + uChromaticSpread));

    // Convert offsets back from optical aspect space to standard UV
    rOffset /= aspect;
    gOffset /= aspect;
    bOffset /= aspect;

    // Boundary edge refraction disturbance
    vec2 normalTangent = vec2(-facetDir.y, facetDir.x) * ((1.0 - facetEdgeLine) * 0.015);
    rOffset += normalTangent;
    bOffset -= normalTangent;

    vec2 uvR = clamp(vTexCoord + rOffset, 0.001, 0.999);
    vec2 uvG = clamp(vTexCoord + gOffset, 0.001, 0.999);
    vec2 uvB = clamp(vTexCoord + bOffset, 0.001, 0.999);

    float red   = texture(uInputTexture, uvR).r;
    float green = texture(uInputTexture, uvG).g;
    float blue  = texture(uInputTexture, uvB).b;
    float alpha = texture(uInputTexture, uvG).a;

    // Subtle glass bevel edge reflection
    float glassBevelGleam = (1.0 - facetEdgeLine) * 0.12 * radialFalloff;
    vec3 color = vec3(red, green, blue) + vec3(glassBevelGleam);

    fragColor = vec4(clamp(color, 0.0, 1.0), alpha);
}
