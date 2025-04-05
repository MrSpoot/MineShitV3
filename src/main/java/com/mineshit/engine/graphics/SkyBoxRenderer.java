package com.mineshit.engine.graphics;

import com.mineshit.engine.window.Window;
import com.mineshit.game.world.ChunkRenderable;
import com.mineshit.game.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL30C.*;

public class SkyBoxRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkyBoxRenderer.class);

    private Shader shader;
    private int vao;
    private float timeOfDay = 0; // 60 secondes = 1 cycle complet

    public void init() {
        LOGGER.info("Initializing SkyBoxRenderer");
        shader = new Shader("/shaders/skybox.glsl");
        vao = glGenVertexArrays();
    }

    public void render(Camera camera,World world, float alpha){

        this.timeOfDay = (System.currentTimeMillis() % 60000L) / 60000f;

        shader.useProgram();

        shader.setUniform("timeOfDay", timeOfDay);

        shader.setUniform("aspect", camera.getAspectRatio());
        shader.setUniform("camRight", camera.getRight());
        shader.setUniform("camUp", camera.getUp());
        shader.setUniform("camForward", camera.getForward());

        glDepthMask(false);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);
        glDepthMask(true);

        shader.unbind();

    }

    public void cleanup() {
        shader.destroy();
        glDeleteVertexArrays(vao);
    }
}
