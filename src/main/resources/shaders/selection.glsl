//@vs
#version 330 core

layout(location = 0) in vec3 aPos;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

out vec2 vUv;

void main() {
    vec3 localPos = aPos;
    vUv = localPos.xy;
    gl_Position = uProjection * uView * uModel * vec4(localPos, 1.0);
}
//@endvs

//@fs
#version 330 core

in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uTexture;

void main() {
    vec4 texColor = texture(uTexture, vUv);
    if (texColor.a < 0.1) discard;
    FragColor = texColor;
}
//@endfs
