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
layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec3 gNormal;
layout(location = 2) out vec3 gPosition;

in vec2 vUV;
in flat float vTexIndex;
in flat float vFaceIndex;
in vec3 vWorldPos;

uniform sampler2DArray uTextureArray;

vec2 getFaceUV(vec2 baseUV, int faceIndex) {
    float tileWidth = 1.0 / 6.0;
    return vec2(baseUV.x * tileWidth + tileWidth * float(faceIndex), baseUV.y);
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

    gAlbedo = texColor;
    gNormal = getFaceNormal(int(vFaceIndex));
    gPosition = vWorldPos;
}
//@endfs
