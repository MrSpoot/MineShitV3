package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.graphics.renderer.utils.SkyboxMap;
import com.mineshit.engine.window.Window;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL30C.*;

public class SkyBoxPass implements RenderPass {

    private Shader shader;
    private int vao;
    private SkyboxMap skyboxMap;

    @Override
    public void init(Window window) {
        this.shader = new Shader("/shaders/skybox.glsl");
        this.skyboxMap = new SkyboxMap(window.getWidth(), window.getHeight());
        vao = glGenVertexArrays();
    }

    @Override
    public void render(RenderContext ctx) {
        skyboxMap.bind();

        shader.useProgram();
        shader.setUniform("timeOfDay", ctx.world().getClock().getWorldTime());
        shader.setUniform("aspect", ctx.camera().getAspectRatio());
        shader.setUniform("camRight", ctx.camera().getRight());
        shader.setUniform("camUp", ctx.camera().getUp());
        shader.setUniform("camForward", ctx.camera().getForward());
        shader.setUniform("uFov", (float) Math.toRadians(ctx.camera().getFov()));

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);

        shader.unbind();
        skyboxMap.unbind(ctx.window().getWidth(), ctx.window().getHeight());
    }

    @Override
    public void cleanup() {
        this.shader.destroy();
        skyboxMap.cleanup();
        glDeleteVertexArrays(vao);
    }

    public SkyboxMap getSkyboxMap() {
        return skyboxMap;
    }
}
