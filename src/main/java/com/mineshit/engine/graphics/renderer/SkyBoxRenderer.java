package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.utils.Statistic;
import com.mineshit.game.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL30C.*;

public class SkyBoxRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkyBoxRenderer.class);

    private Shader shader;
    private int vao;

    public void init() {
        LOGGER.info("Initializing SkyBoxRenderer");
        shader = new Shader("/shaders/skybox.glsl");
        vao = glGenVertexArrays();
    }

    public void render(Camera camera, World world, float alpha){
        shader.useProgram();

        shader.setUniform("timeOfDay", world.getClock().getWorldTime());

        shader.setUniform("aspect", camera.getAspectRatio());
        shader.setUniform("camRight", camera.getRight());
        shader.setUniform("camUp", camera.getUp());
        shader.setUniform("camForward", camera.getForward());
        shader.setUniform("uFov", (float) Math.toRadians(camera.getFov()));

        glDepthMask(false);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);
        glDepthMask(true);

        Statistic.increment("Drawcalls");

        shader.unbind();

    }

    public void cleanup() {
        shader.destroy();
        glDeleteVertexArrays(vao);
    }
}
