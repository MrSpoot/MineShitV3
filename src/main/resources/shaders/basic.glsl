//@vs
#version 460 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in float aTexIndex;
layout(location = 3) in float aFaceIndex;

out flat float vTexIndex;
out flat float vFaceIndex;
out vec4 vFragPosLightSpace;
out vec2 vUV;
out vec3 vWorldPos;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
uniform mat4 uLightSpaceMatrix;

void main() {
    vec4 worldPos = uModel * vec4(aPos, 1.0);
    vWorldPos = worldPos.xyz;
    vFragPosLightSpace = uLightSpaceMatrix * worldPos;

    vTexIndex = aTexIndex;
    vFaceIndex = aFaceIndex;
    vUV = aUV;
    gl_Position = uProjection * uView * worldPos;
}
//@endvs

//@fs
#version 460 core
out vec4 FragColor;

in vec2 vUV;
in flat float vTexIndex;
in flat float vFaceIndex;
in vec4 vFragPosLightSpace;
in vec3 vWorldPos;

uniform sampler2DArray uTextureArray;
uniform vec3 uSunDir;
uniform sampler2D uShadowMap;
uniform mat4 uLightSpaceMatrix;

float calculateShadow(vec4 fragPosLightSpace, vec3 worldNormal)
{
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;

    if(projCoords.z > 1.0) return 0.0;

    float currentDepth = projCoords.z;
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(uShadowMap, 0);

    vec3 dynamicNormal = normalize(cross(dFdx(vWorldPos), dFdy(vWorldPos)));
    float bias = max(0.001 * (1.0 - dot(dynamicNormal, normalize(uSunDir))), 0.0002);

    for(int x = -1; x <= 1; ++x)
    {
        for(int y = -1; y <= 1; ++y)
        {
            vec2 offset = vec2(x, y) * texelSize;
            float pcfDepth = texture(uShadowMap, projCoords.xy + offset).r;
            shadow += (currentDepth - bias > pcfDepth) ? 1.0 : 0.0;
        }
    }

    shadow /= 9.0;
    return shadow;
}

vec3 getSunColor(float height) {
    vec3 morningEveningColor = vec3(1.0, 0.6, 0.3);
    vec3 noonColor           = vec3(1.0);

    float t = clamp(height * 2.0, 0.0, 1.0);
    return mix(morningEveningColor, noonColor, t);
}

vec2 getFaceUV(vec2 baseUV, int faceIndex) {
    const float faceCount = 6.0;
    float tileWidth = 1.0 / faceCount;

    vec2 adjustedUV;
    adjustedUV.x = baseUV.x * tileWidth + tileWidth * float(faceIndex);
    adjustedUV.y = baseUV.y;

    return adjustedUV;
}

vec3 getFaceNormal(int faceIndex) {
    return vec3[6](
    vec3(0, 1, 0),   // TOP
    vec3(0, 0, 1),   // FRONT
    vec3(0, 0, -1),  // BACK
    vec3(1, 0, 0),   // RIGHT
    vec3(-1, 0, 0),  // LEFT
    vec3(0, -1, 0)   // BOTTOM
    )[faceIndex];
}

void main() {
    vec4 texColor = texture(uTextureArray, vec3(getFaceUV(vUV, int(vFaceIndex)), vTexIndex - 1));
    if (texColor.a < 0.1) discard;

    vec3 faceNormal = getFaceNormal(int(vFaceIndex));
    vec3 dynamicNormal = normalize(cross(dFdx(vWorldPos), dFdy(vWorldPos)));

    float shadow = calculateShadow(vFragPosLightSpace, dynamicNormal);

    float sunHeight = clamp(-uSunDir.y, 0.0, 1.0);
    float lightIntensity = smoothstep(0.0, 0.3, sunHeight);

    vec3 sunColor = getSunColor(-uSunDir.y);
    vec3 lightColor = texColor.rgb * sunColor;
    float sunDot = max(dot(normalize(faceNormal), -normalize(uSunDir)), 0.5);

    float diffuse = sunDot * lightIntensity;

    float shadowFactor = 1.0 - shadow * lightIntensity * 0.5;

    float brightness = diffuse * shadowFactor;
    brightness = mix(0.3, brightness, lightIntensity);

    FragColor = vec4(lightColor * brightness, texColor.a);
}
//@endfs