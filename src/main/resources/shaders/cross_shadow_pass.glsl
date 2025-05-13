//@vs
#version 460 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec4 aInstanceData; // xyz: position world, w: texture index

uniform mat4 uLightSpaceMatrix;

out vec2 vUV;
out flat float vTexIndex;

void main() {
    vUV = aUV;
    vTexIndex = aInstanceData.w;
    vec4 worldPos = vec4(aPos + aInstanceData.xyz, 1.0);
    gl_Position = uLightSpaceMatrix * worldPos;
}

//@endvs

//@fs
#version 460 core

in vec2 vUV;
in flat float vTexIndex;

uniform sampler2DArray uTextureArray;

// Identique à ton shader de rendu cross : gestion UV sur une face arbitraire (ici face 0)
vec2 getFaceUV(vec2 baseUV) {
    float tileWidth = 1.0 / 6.0;
    return vec2(baseUV.x * tileWidth, baseUV.y);
}

void main() {
    vec4 texColor = texture(uTextureArray, vec3(getFaceUV(vUV), vTexIndex - 1));
    if (texColor.a < 0.1)
    discard;
    // Pas de FragColor car la profondeur est automatiquement gérée par le depth buffer
}

//@endfs