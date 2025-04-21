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

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
uniform mat4 uLightSpaceMatrix;

void main() {
    vec4 worldPos = uModel * vec4(aPos, 1.0);
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

uniform sampler2DArray uTextureArray;
uniform vec3 uSunDir;
uniform sampler2D uShadowMap;
uniform mat4 uLightSpaceMatrix;

float getShadow(vec4 fragPosLightSpace, float bias) {
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;

    if(projCoords.z > 1.0) return 0.0;

    float closestDepth = texture(uShadowMap, projCoords.xy).r;
    float currentDepth = projCoords.z;

    return currentDepth - bias > closestDepth ? 1.0 : 0.0;
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
    if(texColor.a < 0.1) discard;

    vec3 normal = getFaceNormal(int(vFaceIndex));
    float brightness = max(dot(normalize(normal), normalize(uSunDir)), 0.7);

    // Shadow test
    float bias = 0.005;
    float shadow = getShadow(vFragPosLightSpace, bias);

    float light = max(0.5, shadow); // dans l’ombre → moins de lumière
    FragColor = vec4(texColor.rgb * light, texColor.a);
}
//@endfs