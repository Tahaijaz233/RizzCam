#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform float uThreshold; // Highlight luminance cutoff, e.g., 0.78
uniform float uKnee;      // Soft knee transition width, e.g., 0.15

void main() {
    vec3 color = texture(uInputTexture, vTexCoord).rgb;
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    
    // Quadratic soft-knee extraction curve with division-by-zero defense
    float safeKnee = max(uKnee, 0.0001);
    float kneeMin = uThreshold - safeKnee;
    float kneeMax = uThreshold + safeKnee;
    float weight = smoothstep(kneeMin, kneeMax, luma);
    
    fragColor = vec4(color * weight, 1.0);
}
