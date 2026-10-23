#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uBaseTexture;
uniform sampler2D uHalationBlurTexture;
uniform vec3 uHalationTint;     // Default CineStill red-orange: vec3(1.0, 0.26, 0.08)
uniform float uHalationStrength;// Range: [0.0, 1.0]

void main() {
    vec4 base = texture(uBaseTexture, vTexCoord);
    vec3 blur = texture(uHalationBlurTexture, vTexCoord).rgb;
    
    float blurLuma = dot(blur, vec3(0.299, 0.587, 0.114));
    vec3 tintedHalo = mix(blur, vec3(blurLuma) * uHalationTint * 2.2, 0.85);
    
    // Inverse-luminance weighted screen blend so halation blooms into darker surrounding edges
    vec3 result = 1.0 - (1.0 - base.rgb) * (1.0 - tintedHalo * uHalationStrength);
    fragColor = vec4(clamp(result, 0.0, 1.0), base.a);
}
