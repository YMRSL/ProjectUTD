#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D ThermalSampler;
uniform sampler2D ThermalEntitySampler;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

// 简单伪随机函数
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

// 计算亮度
float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec4 sceneColor = texture(DiffuseSampler, texCoord);
    vec4 thermalBlockColor = textureLod(ThermalSampler, texCoord, 0.0);
    vec4 thermalEntityColor = textureLod(ThermalEntitySampler, texCoord, 0.0);

    // 背景处理
    float sceneLuma = luma(sceneColor.rgb);
    vec3 bgDeep = vec3(0.0);// 黑
    vec3 bgMid  = vec3(0.3);// 深灰
    vec3 bgHigh = vec3(0.7);// 浅灰
    vec3 bgColor = mix(bgDeep, bgMid, smoothstep(0.0, 0.4, sceneLuma));
    bgColor = mix(bgColor, bgHigh, smoothstep(0.4, 1.0, sceneLuma));

    float noise = random(texCoord * 100.0);
    bgColor += (noise - 0.5) * 0.05;

    vec2 uv = texCoord * (1.0 - texCoord.yx);
    float vig = uv.x * uv.y * 15.0;
    vig = pow(vig, 0.25);
    bgColor *= vig;

    vec3 finalColor = bgColor;

    // 环境热源处理
    float warmth = sceneColor.r - max(sceneColor.g, sceneColor.b);
    float brightHeat = smoothstep(0.92, 1.0, sceneLuma);
    float warmHeat = smoothstep(0.5, 0.9, sceneLuma) * smoothstep(0.05, 0.4, warmth);
    float envHeat = max(brightHeat, warmHeat);

    if (envHeat > 0.01) {
        vec3 envColor = mix(vec3(0.3), vec3(1.0), envHeat);// 灰->白
        finalColor = mix(finalColor, envColor, clamp(envHeat + 0.4, 0.0, 1.0));
    }

    // 方块热源处理
    vec3 blockSample = clamp((thermalBlockColor.rgb - vec3(0.5)) * 1.35 + vec3(0.5), 0.0, 1.0);

    float blockMax = max(max(blockSample.r, blockSample.g), blockSample.b);
    float blockMin = min(min(blockSample.r, blockSample.g), blockSample.b);
    float blockSat = blockMax - blockMin;
    float blockLuma = luma(blockSample);
    float blockContrast = max(blockMax - sceneLuma, blockSat * 0.5);

    bool hasBlock = (thermalBlockColor.a < 0.99 && blockMax > 0.05)
    || blockContrast > 0.25
    || (blockMax > 0.85 && blockSat > 0.15);
    if (hasBlock) {
        float blockHeat = 0.4 + 0.6 * max(blockMax, blockSat);
        blockHeat = pow(blockHeat, 0.8);

        vec3 colCold = vec3(0.3);
        vec3 colMid  = vec3(0.6);
        vec3 colHot  = vec3(1.0);

        vec3 blockColor;
        if (blockHeat < 0.5) {
            blockColor = mix(colCold, colMid, blockHeat * 2.0);
        } else {
            blockColor = mix(colMid, colHot, (blockHeat - 0.5) * 2.0);
        }

        finalColor = blockColor;
    }
    float entityMax = max(max(thermalEntityColor.r, thermalEntityColor.g), thermalEntityColor.b);
    bool hasEntity = thermalEntityColor.a > 0.01 && entityMax > 0.05;
    if (hasEntity) {
        float entityLuma = luma(thermalEntityColor.rgb);
        float intensity = smoothstep(0.1, 0.8, entityLuma) * 0.4 + 0.6;
        vec3 entityColor = vec3(intensity);
        finalColor = entityColor;
    }

    float filmNoise = random(texCoord * 900.0 + Time * 6.0) - 0.5;
    float scanLine = sin((texCoord.y + Time * 0.05) * 360.0) * 0.02;
    float grain = clamp(filmNoise * 0.12 + scanLine, -0.08, 0.08);
    finalColor = clamp(finalColor + grain, 0.0, 1.0);

    fragColor = vec4(finalColor, 1.0);
}
