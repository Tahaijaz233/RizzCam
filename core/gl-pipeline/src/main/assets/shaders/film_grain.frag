#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform vec2 uResolution;
uniform float uTimeSeed;       // Frame-randomized seed: fract(time * 43.17)
uniform float uGrainIntensity; // Range: [0.0, 0.35]
uniform float uGrainSize;      // Particle scale factor: [1.0 (35mm fine) to 3.5 (110/8mm coarse)]
uniform float uColorGrain;     // 0.0 = Monochrome silver grain, 1.0 = Dye-cloud chroma noise

// Fast GPU 2D Simplex Noise (Ashima / Ian McEwan)
vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec2 mod289(vec2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec3 permute(vec3 x) { return mod289(((x * 34.0) + 1.0) * x); }

float snoise(vec2 v) {
    const vec4 C = vec4(
        0.211324865405187,  // (3.0 - sqrt(3.0)) / 6.0
        0.366025403784439,  // 0.5 * (sqrt(3.0) - 1.0)
       -0.577350269189626,  // -1.0 + 2.0 * C.x
        0.024390243902439   // 1.0 / 41.0
    );
    vec2 i  = floor(v + dot(v, C.yy));
    vec2 x0 = v - i + dot(i, C.xx);
    vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod289(i);
    vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
    vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
    m = m * m;
    m = m * m;
    vec3 x = 2.0 * fract(p * C.www) - 1.0;
    vec3 h = abs(x) - 0.5;
    vec3 ox = floor(x + 0.5);
    vec3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
    vec3 g;
    g.x  = a0.x  * x0.x  + h.x  * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return 130.0 * dot(m, g);
}

// Pegtop continuous Soft-Light blend (eliminates derivative discontinuity at 0.5)
vec3 blendSoftLight(vec3 base, vec3 blend) {
    return (1.0 - 2.0 * blend) * base * base + 2.0 * blend * base;
}

void main() {
    vec4 baseColor = texture(uInputTexture, vTexCoord);
    
    // Normalize grain coordinate space relative to a reference 1080p short edge
    // so grain size remains identical between 1080p preview and 4000x3000 photo capture!
    float minDim = min(uResolution.x, uResolution.y);
    vec2 normCoord = (vTexCoord * uResolution) / (minDim / 1080.0);
    vec2 grainUv = (normCoord / max(uGrainSize, 0.5)) + vec2(uTimeSeed * 173.3, uTimeSeed * 291.7);
    
    // Two-octave organic silver-halide clumping
    float nPrimary = snoise(grainUv * 0.65);
    float nFine    = snoise(grainUv * 1.45 + vec2(19.1, 47.3));
    float monoGrain = (nPrimary * 0.65 + nFine * 0.35);
    
    // Optional dye-cloud chroma noise for high-ISO color negative stocks (e.g., Superia 800)
    vec3 chromaGrain = vec3(
        monoGrain,
        snoise(grainUv * 0.65 + vec2(53.2, 11.8)),
        snoise(grainUv * 0.65 + vec2(91.4, 83.1))
    );
    vec3 grainSample = mix(vec3(monoGrain), chromaGrain, uColorGrain);
    
    // Emulsion luminance response curve: peak in midtones, suppressed in deep shadows & speculars
    float luma = dot(baseColor.rgb, vec3(0.2126, 0.7152, 0.0722));
    float shadowMask = smoothstep(0.0, 0.12, luma);
    float highlightMask = 1.0 - smoothstep(0.75, 0.98, luma);
    float midtoneWeight = shadowMask * highlightMask;
    
    // Map [-1, 1] noise to [0, 1] neutral-gray centered (0.5) overlay space
    vec3 blendLayer = clamp(vec3(0.5) + grainSample * uGrainIntensity * midtoneWeight, 0.0, 1.0);
    vec3 grainedRgb = blendSoftLight(baseColor.rgb, blendLayer);
    
    fragColor = vec4(clamp(grainedRgb, 0.0, 1.0), baseColor.a);
}
