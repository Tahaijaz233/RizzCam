#version 300 es
precision highp float;

layout(location = 0) in vec2 aPosition; // Normalized Device Coordinates: [-1.0, 1.0]
layout(location = 1) in vec2 aTexCoord; // Standard UV Coordinates: [0.0, 1.0]

uniform mat4 uTexMatrix; // Identity for standard 2D passes; Sensor*Crop matrix for Pass 0

out vec2 vTexCoord;

void main() {
    gl_Position = vec4(aPosition, 0.0, 1.0);
    vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
}
