#version 150

// 能量核心片段著色器 - 實現中心發光效果
uniform float GameTime;
uniform float CoreSize;
uniform float CoreIntensity;
uniform float PulseSpeed;
uniform vec3 CoreColor;

in vec4 vertexColor;
in vec2 screenPos;

out vec4 fragColor;

// 平滑步函數
float smoothstep_custom(float edge0, float edge1, float x) {
    float t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

// 噪聲函數（簡化版）
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    
    vec2 u = f * f * (3.0 - 2.0 * f);
    
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

void main() {
    // 將屏幕坐標轉換為以中心為原點的坐標
    vec2 uv = (screenPos - 0.5) * 2.0;  // 範圍 [-1, 1]
    float dist = length(uv);
    
    // 基礎發光核心
    float coreGlow = 1.0 / (1.0 + dist * dist * 8.0);
    
    // 脈動效果
    float pulse = 0.8 + 0.3 * sin(GameTime * PulseSpeed);
    coreGlow *= pulse;
    
    // 添加噪聲以產生能量波動效果
    float energyNoise = noise(uv * 15.0 + GameTime * 2.0) * 0.3 + 0.7;
    coreGlow *= energyNoise;
    
    // 內部明亮區域
    float innerBrightness = smoothstep_custom(0.6, 0.0, dist) * 2.0;
    
    // 外部輝光
    float outerGlow = smoothstep_custom(1.2, 0.3, dist) * 0.8;
    
    // 組合所有效果
    float totalIntensity = (coreGlow + innerBrightness + outerGlow) * CoreIntensity;
    
    // 計算最終顏色
    vec3 finalColor = CoreColor * totalIntensity;
    
    // 中心區域更偏向白色
    if (dist < 0.2) {
        finalColor = mix(finalColor, vec3(1.0, 1.0, 1.0), (0.2 - dist) * 3.0);
    }
    
    // 輸出顏色，alpha用於混合
    fragColor = vec4(finalColor, min(totalIntensity, 1.0));
}