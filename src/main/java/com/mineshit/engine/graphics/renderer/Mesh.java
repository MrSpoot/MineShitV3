package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.utils.Statistic;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {
    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int vertexCount;

    public Mesh(FloatBuffer vertexBuffer, IntBuffer indexBuffer, int vertexSize) {
        this.vertexCount = indexBuffer.remaining();

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        // VBO
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        // EBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        // Vertex Attributes
        int stride = vertexSize * Float.BYTES;

        glEnableVertexAttribArray(0); // aPos
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);

        glEnableVertexAttribArray(1); // aUV
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);

        glEnableVertexAttribArray(2); // aTexIndex
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5 * Float.BYTES);

        glEnableVertexAttribArray(3); // aFaceIndex
        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 6 * Float.BYTES);

        glBindVertexArray(0);
    }

    public void render() {
        if(vertexCount > 0){
            glBindVertexArray(vao);
            glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
            Statistic.increment("Drawcalls");
        }
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }
}
