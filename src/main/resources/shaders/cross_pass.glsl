//@vs
#version 460 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec4 aInstanceData;

uniform mat4 uProjection;
uniform mat4 uView;

out vec2 vUV;
out vec3 vWorldPos;
out flat float vTexIndex;

void main() {
    vUV = aUV;
    vTexIndex = aInstanceData.w;
    vec4 worldPos = vec4(aPos + aInstanceData.xyz, 1.0);
    vWorldPos = worldPos.xyz;
    gl_Position = uProjection * uView * worldPos;
}
//@endvs

//@fs
#version 460 core

uniform sampler2DArray uTextureArray;
in vec2 vUV;
in vec3 vWorldPos;
in flat float vTexIndex;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec3 gNormal;
layout(location = 2) out vec3 gPosition;

vec2 getFaceUV(vec2 baseUV, int faceIndex) {
    float tileWidth = 1.0 / 6.0;
    return vec2(baseUV.x * tileWidth + tileWidth * float(faceIndex), baseUV.y);
}

void main() {
    vec4 texColor = texture(uTextureArray, vec3(getFaceUV(vUV, int(0)), vTexIndex - 1));
    if (texColor.a < 0.1) discard;
    gAlbedo = texColor;
    gPosition = vWorldPos;
    gNormal = vec3(0.0, 1.0, 0.0);
}

//@endfs