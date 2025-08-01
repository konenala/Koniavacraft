#version 150

// 能量核心頂點著色器
in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 screenPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    
    // 屏幕空間坐標，用於計算發光效果
    screenPos = (gl_Position.xy / gl_Position.w) * 0.5 + 0.5;
}