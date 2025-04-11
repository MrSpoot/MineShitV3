package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.utils.Image;
import com.mineshit.engine.utils.Statistic;
import com.mineshit.game.world.World;
import com.mineshit.game.world.interaction.HitResult;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

public class SelectionRenderer {
    private static Mesh mesh;
    private Shader shader;
    private int textureId;

    public void init() {
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(8 * 3);
        vertexBuffer.put(new float[]{
                0, 0, 0, // 0
                1, 0, 0, // 1
                1, 1, 0, // 2
                0, 1, 0, // 3
                0, 0, 1, // 4
                1, 0, 1, // 5
                1, 1, 1, // 6
                0, 1, 1  // 7
        });
        vertexBuffer.flip();

        // 6 faces × 2 triangles × 3 indices = 36 indices
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(36);
        indexBuffer.put(new int[]{
                // back face (z = 0)
                0, 3, 2,
                2, 1, 0,

                // front face (z = 1)
                4, 5, 6,
                6, 7, 4,

                // left face (x = 0)
                0, 4, 7,
                7, 3, 0,

                // right face (x = 1)
                1, 2, 6,
                6, 5, 1,

                // bottom face (y = 0)
                0, 1, 5,
                5, 4, 0,

                // top face (y = 1)
                3, 7, 6,
                6, 2, 3
        });
        indexBuffer.flip();

        shader = new Shader("/shaders/selection.glsl");

        mesh = new Mesh(vertexBuffer, indexBuffer, 3);

        createTexture();

        MemoryUtil.memFree(vertexBuffer);
        MemoryUtil.memFree(indexBuffer);
    }

    public void render(Camera camera, World world, float alpha) {
        if (mesh == null) return;

        HitResult r = world.getInteraction().getHitResult();
        if (r == null || r.blockPos() == null) return;

        Matrix4f model = new Matrix4f()
                .translate(r.blockPos().x + 0.5f, r.blockPos().y + 0.5f, r.blockPos().z + 0.5f)
                .scale(1.001f)
                .translate(-0.5f, -0.5f, -0.5f);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.useProgram();
        shader.setUniform("uProjection", camera.getProjectionMatrix());
        shader.setUniform("uView", camera.getViewMatrix());
        shader.setUniform("uModel", model);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        shader.setUniform("uTexture", 0);
        
        mesh.render();

        Statistic.increment("Drawcalls");

        shader.unbind();

        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_BLEND);
    }

    public void cleanup() {
        if (mesh != null) mesh.cleanup();
        shader.destroy();
        if (textureId != 0) {
            glDeleteTextures(textureId);
        }
    }

    private void createTexture(){
        Image img = new Image(new Vector4f(1.0f, 1.0f, 1.0f, 0.2f));
        
        ByteBuffer buffer = img.getByteBuffer();
        int width = img.getWidth();
        int height = img.getHeight();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);

    }
}

