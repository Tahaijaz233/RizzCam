#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform sampler2D uLightLeakTexture;
uniform sampler2D uBorderMaskTexture;

uniform vec2 uResolution;
uniform float uVignetteIntensity; // [0.0, 1.5]
uniform float uVignetteSmoothness;// [0.2, 1.0]
uniform float uLightLeakOpacity;  // [0.0, 1.0]
uniform vec2 uLightLeakUvTransform;// Allows flipping/mirroring leak assets randomly per shot
uniform int uEnableBorderMask;    // 0 = disabled, 1 = enabled

void main() {
    vec4 color = texture(uInputTexture, vTexCoord);
    
    // 1. Optical Vignette Falloff
    vec2 aspect = vec2(uResolution.x / min(uResolution.x, uResolution.y),
                       uResolution.y / min(uResolution.x, uResolution.y));
    vec2 centered = (vTexCoord - 0.5) * aspect;
    float dist = length(centered);
    float vignette = smoothstep(
        0.85 * uVignetteSmoothness + 0.35,
        0.20 * uVignetteSmoothness,
        dist * uVignetteIntensity
    );
    vec3 composed = color.rgb * vignette;
    
    // 2. Organic Light Leak (Screen Blend)
    if (uLightLeakOpacity > 0.001) {
        vec2 leakUv = abs(uLightLeakUvTransform - vTexCoord);
        vec3 leak = texture(uLightLeakTexture, leakUv).rgb * uLightLeakOpacity;
        composed = 1.0 - (1.0 - composed) * (1.0 - leak);
    }
    
    // 3. Retro Film Border / Sprocket / Polaroid Frame Mask (Premultiplied Alpha Overlay)
    if (uEnableBorderMask == 1) {
        vec4 border = texture(uBorderMaskTexture, vTexCoord);
        composed = mix(composed, border.rgb, border.a);
    }
    
    fragColor = vec4(clamp(composed, 0.0, 1.0), 1.0);
}
