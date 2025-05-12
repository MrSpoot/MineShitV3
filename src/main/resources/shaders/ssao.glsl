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
layout(location = 0) out vec4 FragColor;

uniform sampler2D uPosition;
uniform sampler2D uNormal;
uniform vec2 uTexSize;
uniform vec3 uCameraPos;


const int SAMPLES = 128;

#define PI 3.14159265
#define GOLDEN_ANGLE 2.39996322973 // radians (~137.5°)
#define SAMPLE_RADIUS 1.2    // Distance en mètres : ~1.2m = couvre jusqu'à un coin de bloc voisin
#define INTENSITY     5.0    // Ombre plus marquée pour bien contraster
#define SCALE         1.2    // Atténue légèrement avec la distance
#define BIAS          0.01   // Très faible, pour détecter même de petits creux
#define MAX_DISTANCE  25.0    // Rayon max pour valider une occlusion (~1.5 blocs)

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453);
}

vec3 getPosition(vec2 uv) {
    return texture(uPosition, uv).xyz;
}

vec3 getNormal(vec2 uv) {
    return normalize(texture(uNormal, uv).xyz);
}

float doAO(vec2 sampleUV, vec3 centerPos, vec3 normal) {
    vec3 samplePos = getPosition(sampleUV);
    vec3 offset = samplePos - centerPos;
    float dist = length(offset);
    if (dist < 0.0001) return 0.0;

    vec3 dir = normalize(offset);
    float angle = max(dot(normal, dir) - BIAS, 0.0);
    float atten = 1.0 / (1.0 + dist * SCALE);
    float _smooth = smoothstep(MAX_DISTANCE, MAX_DISTANCE * 0.5, dist);
    return angle * atten * _smooth;
}

void main() {
    vec3 centerPos = getPosition(vUV);
    vec3 centerNormal = getNormal(vUV);

    if (length(centerNormal) < 0.1) {
        FragColor = vec4(1.0);
        return;
    }

    float depth = length(centerPos - uCameraPos);
    float radius = SAMPLE_RADIUS;

    float ao = 0.0;
    float angle = rand(vUV) * 2.0 * PI;
    float step = radius / float(SAMPLES);

    for (int i = 0; i < SAMPLES; ++i) {
        float t = float(i);
        float r = t * step;
        vec2 dir = vec2(sin(angle), cos(angle));
        vec2 sampleUV = vUV + dir * r;

        if (all(greaterThanEqual(sampleUV, vec2(0.0))) &&
        all(lessThanEqual(sampleUV, vec2(1.0)))) {
            ao += doAO(sampleUV, centerPos, centerNormal);
        }

        angle += GOLDEN_ANGLE;
    }

    ao = 1.0 - ao * (1.0 / float(SAMPLES)) * INTENSITY;

    float distanceFade = smoothstep(5.0, 15.0, depth);
    ao = mix(ao, 1.0, distanceFade);

    FragColor = vec4(vec3(ao), 1.0);
}
//@endfs
