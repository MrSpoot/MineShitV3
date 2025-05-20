//@vs
#version 460 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in float aTexIndex;
layout(location = 3) in float aFaceIndex;
layout(location = 4) in float aFaceSizeU;
layout(location = 5) in float aFaceSizeV;

out flat float vTexIndex;
out flat float vFaceIndex;
out vec2 vUV;
out vec2 vFaceSize;
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
    vFaceSize = vec2(aFaceSizeU, aFaceSizeV);
    gl_Position = uProjection * uView * worldPos;
}
//@endvs

//@fs
#version 460 core
out vec4 FragColor;

in vec2 vUV;
in flat float vTexIndex;
in flat float vFaceIndex;
in vec2 vFaceSize;
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

vec2 getFaceUV(vec2 baseUV, int faceIndex, vec2 faceSize) {
    float tileWidth = 1.0 / 6.0;

    vec2 repeatedUV = baseUV * faceSize;

    repeatedUV.x = mod(repeatedUV.x, 1.0) * tileWidth + tileWidth * float(faceIndex);
    repeatedUV.y = mod(repeatedUV.y, 1.0);

    return repeatedUV;
}

void main() {
    vec2 uv = getFaceUV(vUV, int(vFaceIndex), vFaceSize);
    vec4 texColor = texture(uTextureArray, vec3(uv, vTexIndex - 1));

    if (texColor.a < 0.1) discard;

    gAlbedo = texColor;
    gNormal = getFaceNormal(int(vFaceIndex));
    gPosition = vWorldPos;
}
//@endfs