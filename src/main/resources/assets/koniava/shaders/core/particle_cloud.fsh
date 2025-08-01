#version 150

// 粒子雲片段著色器 - 實現散射粒子效果
uniform float GameTime;
uniform int ParticleCount;
uniform float CloudRadius;
uniform float ParticleSize;
uniform float CloudDensity;
uniform float AnimationSpeed;
uniform vec3 InnerColor;
uniform vec3 OuterColor;

in vec4 vertexColor;
in vec2 texCoord;
in vec2 worldPos;

out vec4 fragColor;

// 改進的隨機函數
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float random(float x) {
    return fract(sin(x * 12.9898) * 43758.5453123);
}

// 多層噪聲函數
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

// 分形噪聲
float fbm(vec2 st) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    
    for (int i = 0; i < 4; i++) {
        value += amplitude * noise(st * frequency);
        amplitude *= 0.5;
        frequency *= 2.0;
    }
    
    return value;
}

// 單個粒子效果
float particle(vec2 pos, vec2 center, float size) {
    float dist = distance(pos, center);
    return 1.0 / (1.0 + (dist / size) * (dist / size) * 20.0);
}

// 主要粒子雲計算
float calculateParticleCloud(vec2 pos) {
    float totalIntensity = 0.0;
    float maxParticles = float(ParticleCount);
    
    // 生成多個粒子
    for (int i = 0; i < 100; i++) { // 固定循環上限以避免著色器問題
        if (float(i) >= maxParticles) break;
        
        float seed = float(i) * 0.1;
        
        // 粒子的基礎位置（隨機分布）
        float angle = random(seed) * 6.28318; // 2π
        float radius = sqrt(random(seed + 0.1)) * CloudRadius; // 平方根分布使粒子更均勻
        
        vec2 basePos = vec2(cos(angle), sin(angle)) * radius;
        
        // 添加時間動畫（旋轉和波動）
        float rotationSpeed = 0.5 + random(seed + 0.2) * 1.0;
        float currentAngle = angle + GameTime * AnimationSpeed * rotationSpeed;
        
        // 徑向波動
        float radialWave = sin(GameTime * 2.0 + radius * 0.5 + seed * 10.0) * 0.3;
        float animatedRadius = radius + radialWave;
        
        vec2 particlePos = vec2(cos(currentAngle), sin(currentAngle)) * animatedRadius;
        
        // 添加噪聲擾動
        vec2 noiseOffset = vec2(
            noise(particlePos * 0.5 + GameTime * 0.3) - 0.5,
            noise(particlePos * 0.5 + GameTime * 0.3 + vec2(100.0, 50.0)) - 0.5
        ) * 0.4;
        
        particlePos += noiseOffset;
        
        // 粒子大小變化
        float sizeVariation = 0.7 + 0.6 * sin(GameTime * 3.0 + seed * 15.0);
        float currentSize = ParticleSize * sizeVariation;
        
        // 計算粒子強度
        float intensity = particle(pos, particlePos, currentSize);
        
        // 距離衰減
        float distFromCenter = length(particlePos);
        float distanceFade = 1.0 - smoothstep(CloudRadius * 0.7, CloudRadius * 1.2, distFromCenter);
        
        totalIntensity += intensity * distanceFade;
    }
    
    return totalIntensity * CloudDensity;
}

void main() {
    vec2 pos = worldPos;
    float distFromCenter = length(pos);
    
    // 計算粒子雲強度
    float cloudIntensity = calculateParticleCloud(pos);
    
    // 添加背景雲霧效果
    float backgroundCloud = fbm(pos * 0.8 + GameTime * 0.2) * 0.3;
    cloudIntensity += backgroundCloud;
    
    // 整體雲團形狀（徑向衰減）
    float cloudShape = 1.0 - smoothstep(CloudRadius * 0.5, CloudRadius * 1.3, distFromCenter);
    cloudIntensity *= cloudShape;
    
    // 顏色計算 - 基於距離中心的遠近
    float colorMix = smoothstep(0.0, CloudRadius * 0.8, distFromCenter);
    vec3 particleColor = mix(InnerColor, OuterColor, colorMix);
    
    // 添加能量波動效果
    float energyPulse = 0.8 + 0.4 * sin(GameTime * 1.5 + distFromCenter * 0.5);
    particleColor *= energyPulse;
    
    // 最終顏色
    vec3 finalColor = particleColor * cloudIntensity;
    
    // 透明度處理
    float alpha = min(cloudIntensity * 0.8, 0.9);
    
    fragColor = vec4(finalColor, alpha);
}