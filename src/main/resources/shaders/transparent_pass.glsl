//@vs
#version 460 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in float aTexIndex;
layout(location = 3) in float aFaceIndex;

out flat float vTexIndex;
out flat float vFaceIndex;
out vec2 vUV;
out vec3 vWorldPos;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

void main() {
    vec4 worldPos = uModel * vec4(aPos, 1.0);
    vWorldPos = worldPos.xyz;

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
in vec3 vWorldPos;

uniform sampler2DArray uTextureArray;

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

    FragColor = vec4(texColor);
}
//@endfs