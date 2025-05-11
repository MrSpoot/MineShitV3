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
//@fs
#version 460 core

in vec2 vUV;
out vec4 FragColor;

uniform sampler2D uOpaqueColor;
uniform sampler2D uOpaqueDepth;
uniform sampler2D uTransparentColor;
uniform sampler2D uTransparentDepth;

void main() {
    vec4 opaqueColor = texture(uOpaqueColor, vUV);
    float opaqueDepth = texture(uOpaqueDepth, vUV).r;

    vec4 transparentColor = texture(uTransparentColor, vUV);
    float transparentDepth = texture(uTransparentDepth, vUV).r;

    if (opaqueDepth >= 1.0 && transparentDepth >= 1.0) {
        discard;
    }

    if (transparentDepth >= 1.0) {
        FragColor = opaqueColor;
        return;
    }

    if (opaqueDepth >= 1.0) {
        FragColor = transparentColor;
        return;
    }

    if (transparentDepth < opaqueDepth) {
        FragColor.rgb = mix(opaqueColor.rgb, transparentColor.rgb, transparentColor.a);
        FragColor.a = 1.0;
    } else {
        FragColor = opaqueColor;
    }
}
//@endfs

