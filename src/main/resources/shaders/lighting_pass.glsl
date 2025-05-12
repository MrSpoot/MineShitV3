//@vs
#version 460 core

out vec2 vUV;

void main() {
    vec2 pos = vec2((gl_VertexID & 1) << 2, (gl_VertexID & 2));
    vUV = pos;
    gl_Position = vec4(pos * 2.0 - 1.0, 0.0, 1.0);
}
//@endvs

//@fs
#version 460 core

in vec2 vUV;
layout(location = 0) out vec4 FragColor;

uniform sampler2D uNormal;
uniform sampler2D uPosition;
uniform sampler2D uSSAO;
uniform sampler2D uShadow;

uniform vec3 uSunDir;
uniform mat4 uLightSpaceMatrix;

float calculateShadow(vec4 fragPosLightSpace, vec3 normal) {
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;

    if (projCoords.z > 1.0) return 0.0;

    float currentDepth = projCoords.z;
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(uShadow, 0);

    float bias = max(0.001 * (1.0 - dot(normal, -normalize(uSunDir))), 0.0002);

    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            vec2 offset = vec2(x, y) * texelSize;
            float pcfDepth = texture(uShadow, projCoords.xy + offset).r;
            shadow += (currentDepth - bias > pcfDepth) ? 1.0 : 0.0;
        }
    }

    shadow /= 9.0;

    return shadow;
}

vec3 getSunColor(float height) {
    vec3 morningEveningColor = vec3(1.0, 0.6, 0.3);
    vec3 noonColor = vec3(1.0);
    float t = clamp(height * 2.0, 0.0, 1.0);
    return mix(morningEveningColor, noonColor, t);
}

void main() {
    vec3 normal = normalize(texture(uNormal, vUV).xyz);
    vec3 position = texture(uPosition, vUV).xyz;
    float ssao = texture(uSSAO, vUV).r;

    vec4 fragPosLightSpace = uLightSpaceMatrix * vec4(position, 1.0);
    float shadow = calculateShadow(fragPosLightSpace, normal);

    float sunHeight = clamp(-uSunDir.y, 0.0, 1.0);
    float lightIntensity = smoothstep(0.0, 0.3, sunHeight);
    vec3 sunColor = getSunColor(-uSunDir.y);
    vec3 lightColor = sunColor;
    float sunDot = max(dot(normal, -normalize(uSunDir)), 0.5);

    float diffuse = sunDot * lightIntensity;
    float shadowFactor = 1.0 - shadow * lightIntensity * 0.5;

    float brightness = diffuse * shadowFactor;
    brightness = mix(0.3, brightness, lightIntensity);

    vec3 lighting = lightColor * brightness * ssao;

    FragColor = vec4(lighting, 1.0);
}
//@endfs
