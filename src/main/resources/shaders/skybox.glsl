//@vs
#version 460 core

out vec2 vUv;

void main() {
    vec2 pos = vec2((gl_VertexID & 1) << 2, (gl_VertexID & 2));
    vUv = pos;
    gl_Position = vec4(pos * 2.0 - 1.0, -1.0, 1.0);
}
//@endvs

//@fs
#version 460 core

uniform vec3 camRight;
uniform vec3 camUp;
uniform vec3 camForward;
uniform float uFov;
uniform float aspect;
uniform float timeOfDay;

in vec2 vUv;
layout(location = 0) out vec4 FragColor;

vec3 vNightColor   = vec3(0.15, 0.3, 0.6);
vec3 vHorizonColor = vec3(0.6, 0.3, 0.4);
vec3 vDayColor     = vec3(0.7, 0.8, 1.0);

vec3 vSunColor     = vec3(1.0, 0.8, 0.6);
vec3 vSunRimColor  = vec3(1.0, 0.66, 0.33);

// Petite fonction de bruit pour les étoiles
float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.71, 0.113, 0.419));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

void main() {
    // Ray direction
    vec2 p = vUv * 2.0 - 1.0;
    p.x *= aspect;

    // Projection plane distance en fonction du FOV vertical
    float z = 1.0 / tan(uFov * 0.5);

    // Direction en espace caméra
    vec3 rayCam = normalize(vec3(p, z));

    // Transforme en world space
    vec3 rd = normalize(
    rayCam.x * camRight +
    rayCam.y * camUp +
    rayCam.z * camForward
    );

    float sunAngle = -timeOfDay * 2.0 * 3.14159;
    vec3 sundir = normalize(vec3(cos(sunAngle) * 0.95, sin(sunAngle),cos(sunAngle) * 0.4));

    float sunDot = clamp(dot(sundir, rd), 0.0, 1.0);

    float sunHeight = sundir.y;

    float fNightHeight = -0.8;
    float fDayHeight = 0.3;
    float fHorizonLength = fDayHeight - fNightHeight;
    float fHalfHorizonLength = fHorizonLength / 2.0;
    float fMidPoint = fNightHeight + fHalfHorizonLength;

    float fNightContrib   = clamp((sunHeight - fMidPoint) * (-1.0 / fHalfHorizonLength), 0.0, 1.0);
    float fHorizonContrib = -clamp(abs((sunHeight - fMidPoint) * (-1.0 / fHalfHorizonLength)), 0.0, 1.0) + 1.0;
    float fDayContrib     = clamp((sunHeight - fMidPoint) * ( 1.0 / fHalfHorizonLength), 0.0, 1.0);

    vec3 skyColor = vec3(0.0);
    skyColor += vNightColor   * fNightContrib;
    skyColor += vHorizonColor * fHorizonContrib;
    skyColor += vDayColor     * fDayContrib;

    skyColor -= clamp(rd.y, 0.0, 0.5);

    skyColor += 0.4 * vSunRimColor * pow(sunDot, 4.0);
    skyColor += 1.0 * vSunColor    * pow(sunDot, 2000.0);

    float starNoise = hash(floor(rd * 400.0));
    float star = smoothstep(0.997, 1.0, starNoise);

    float starIntensity = (1.0 - clamp(sunHeight * 4.0, 0.0, 1.0));

    skyColor += vec3(star) * starIntensity;

    FragColor = vec4(skyColor, 1.0);
}
//@endfs
