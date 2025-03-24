//@vs
#version 460 core
layout(location = 0) in vec3 aPos;

uniform mat4 uProjection;
uniform mat4 uView;

void main() {
    gl_Position = uProjection * uView * vec4(aPos, 1.0);
}
//@endvs

//@fs
#version 460 core
out vec4 FragColor;

void main() {
    FragColor = vec4(1.0, 0.5, 0.2, 1.0); // orange
}
//@endfs
