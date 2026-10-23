#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uInputTexture;
uniform vec2 uTexelOffset; // (1.0/width * radius, 0.0) for H-pass; (0.0, 1.0/height * radius) for V-pass

void main() {
    vec2 off1 = vec2(1.3846153846) * uTexelOffset;
    vec2 off2 = vec2(3.2307692308) * uTexelOffset;
    
    vec3 sum = texture(uInputTexture, vTexCoord).rgb * 0.2270270270;
    sum += texture(uInputTexture, vTexCoord + off1).rgb * 0.3162162162;
    sum += texture(uInputTexture, vTexCoord - off1).rgb * 0.3162162162;
    sum += texture(uInputTexture, vTexCoord + off2).rgb * 0.0702702703;
    sum += texture(uInputTexture, vTexCoord - off2).rgb * 0.0702702703;
    
    fragColor = vec4(sum, 1.0);
}
