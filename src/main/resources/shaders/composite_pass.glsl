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
#version 460 core

in vec2 vUV;
out vec4 FragColor;

uniform sampler2D uAlbedo;
uniform sampler2D uLighting;
uniform sampler2D uDepth;
uniform sampler2D uSkybox;

void main() {
    float depth = texture(uDepth, vUV).r;

    vec3 albedo = texture(uAlbedo, vUV).rgb;
    vec3 lighting = texture(uLighting, vUV).rgb;

    vec3 finalColor = albedo * lighting;

    if (depth >= 1.0) {
        vec3 skyColor = texture(uSkybox, vUV).rgb;
        finalColor = skyColor;
    }

    FragColor = vec4(finalColor, 1.0);
}
//@endfs
