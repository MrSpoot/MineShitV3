//@vs
#version 460 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in float aTexIndex;
layout(location = 3) in float aFaceIndex;

out flat float vTexIndex;
out flat float vFaceIndex;
out vec2 vUV;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

void main() {
    vTexIndex = aTexIndex;
    vFaceIndex = aFaceIndex;
    vUV = aUV;
    gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
}
//@endvs

//@fs
#version 460 core
out vec4 FragColor;

in vec2 vUV;
in flat float vTexIndex;
in flat float vFaceIndex;

uniform sampler2DArray uTextureArray;

vec2 getFaceUV(vec2 baseUV, int faceIndex) {
    const float faceCount = 6.0;
    float tileWidth = 1.0 / faceCount;

    vec2 adjustedUV;
    adjustedUV.x = baseUV.x * tileWidth + tileWidth * float(faceIndex);
    adjustedUV.y = baseUV.y;

    return adjustedUV;
}


void main() {
    FragColor = texture(uTextureArray, vec3(getFaceUV(vUV,int(vFaceIndex)), vTexIndex - 1));
}
//@endfs