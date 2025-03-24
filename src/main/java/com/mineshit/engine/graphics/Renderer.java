package com.mineshit.engine.graphics;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {

    private Shader shader;
    private Mesh triangle;

    public void init() {
        shader = new Shader("/shaders/basic.glsl");

        float[] vertices = {
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
                0.0f,  0.5f, 0.0f
        };

        triangle = new Mesh(vertices);
    }

    public void render(Camera camera, float alpha) {
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.useProgram();

        shader.setUniform("uProjection", camera.getProjectionMatrix());
        shader.setUniform("uView", camera.getViewMatrix());

        triangle.render();

        shader.unbind();
    }

    public void cleanup() {
        shader.destroy();
        triangle.destroy();
    }
}
