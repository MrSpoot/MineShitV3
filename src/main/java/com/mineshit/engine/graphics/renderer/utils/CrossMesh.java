package com.mineshit.engine.graphics.renderer.utils;

import com.mineshit.engine.utils.Statistic;

import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL33C.*;

public class CrossMesh {

    private static int vao;
    private static int vbo;
    private static int ebo;
    private static int vertexCount;

    public static void init() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        // Cross mesh = 2 quads en croix (8 vertices, 12 indices)
        float[] vertices = {
                // Quad 1 (diagonale -45°)
                -0.5f, 0f, -0.5f, 0f, 0f,
                0.5f, 0f,  0.5f, 1f, 0f,
                0.5f, 1f,  0.5f, 1f, 1f,
                -0.5f, 1f, -0.5f, 0f, 1f,

                // Quad 2 (diagonale 45°)
                -0.5f, 0f,  0.5f, 0f, 0f,
                0.5f, 0f, -0.5f, 1f, 0f,
                0.5f, 1f, -0.5f, 1f, 1f,
                -0.5f, 1f,  0.5f, 0f, 1f,
        };


        int[] indices = {
                // Quad 1
                0, 1, 2, 2, 3, 0,
                // Quad 2
                4, 5, 6, 6, 7, 4
        };

        vertexCount = indices.length;

        glBindVertexArray(vao);

        // VBO
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // EBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // Vertex Attributes (pos + uv)
        int stride = 5 * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);

        glBindVertexArray(0);
    }

    public static void bind() {
        glBindVertexArray(vao);
    }

    public static void render(int instanceCount) {
        if (instanceCount > 0) {
            glDrawElementsInstanced(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0, instanceCount);
            Statistic.increment("Drawcalls");
        }
    }

    public static void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }
}
