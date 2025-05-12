package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.GBuffer;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.window.Window;

import static org.lwjgl.opengl.GL11C.glViewport;
import static org.lwjgl.opengl.GL30C.*;

public class CompositePass implements RenderPass {

    private Shader shader;
    private int vao;

    @Override
    public void init(Window window) {
        this.shader = new Shader("/shaders/composite_pass.glsl");
        vao = glGenVertexArrays();
    }

    @Override
    public void render(RenderContext ctx) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, ctx.window().getWidth(), ctx.window().getHeight());
        glClear(GL_COLOR_BUFFER_BIT);

        shader.useProgram();

        ctx.gbuffer().bindTexture(GBuffer.Attachment.ALBEDO, 0);
        ctx.lightingMap().getTexture().bind(1);
        ctx.gbuffer().bindDepthTexture(2);
        ctx.skyboxMap().getTexture().bind(3);

        shader.setUniform("uAlbedo", 0);
        shader.setUniform("uLighting", 1);
        shader.setUniform("uDepth", 2);
        shader.setUniform("uSkybox", 3);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);

        shader.unbind();
    }

    @Override
    public void cleanup() {
        this.shader.destroy();
        glDeleteVertexArrays(vao);
    }
}
