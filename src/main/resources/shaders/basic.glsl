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
uniform vec3 uSunDir;

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
    vec3 normal = getFaceNormal(int(vFaceIndex));
    float brightness = max(dot(normalize(normal), normalize(uSunDir)), 0.7); // 0.2 = minimum lumière

    vec4 texColor = texture(uTextureArray, vec3(getFaceUV(vUV, int(vFaceIndex)), vTexIndex - 1));
    FragColor = vec4(texColor.rgb * brightness, texColor.a);
}
//@endfs