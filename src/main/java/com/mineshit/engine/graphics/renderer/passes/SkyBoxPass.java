package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.utils.Statistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL11C.glDepthMask;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;

public class SkyBoxPass implements RenderPass{
    private static final Logger LOGGER = LoggerFactory.getLogger(SkyBoxPass.class);

    private Shader shader;
    private int vao;

    @Override
    public void init() {
        this.shader = new Shader("/shaders/skybox.glsl");
        vao = glGenVertexArrays();
    }

    @Override
    public void render(RenderContext ctx) {
        shader.useProgram();

        shader.setUniform("timeOfDay", ctx.world().getClock().getWorldTime());

        shader.setUniform("aspect", ctx.camera().getAspectRatio());
        shader.setUniform("camRight", ctx.camera().getRight());
        shader.setUniform("camUp", ctx.camera().getUp());
        shader.setUniform("camForward", ctx.camera().getForward());
        shader.setUniform("uFov", (float) Math.toRadians(ctx.camera().getFov()));

        glDepthMask(false);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);
        glDepthMask(true);

        Statistic.increment("Drawcalls");

        shader.unbind();
    }

    @Override
    public void cleanup() {
        this.shader.destroy();
    }
}
