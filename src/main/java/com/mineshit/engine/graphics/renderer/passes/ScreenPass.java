package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.GBuffer;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.window.Window;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;

public class ScreenPass implements RenderPass {
    private Shader shader;
    private int vao;

    @Override
    public void init(Window window) {
        this.shader = new Shader("/shaders/screen.glsl");
        vao = glGenVertexArrays();
    }

    @Override
    public void render(RenderContext ctx) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, ctx.passes().getFbo());
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

        glBlitFramebuffer(
                0, 0, ctx.gBuffer().getWidth(), ctx.gBuffer().getHeight(),
                0, 0, ctx.gBuffer().getWidth(), ctx.gBuffer().getHeight(),
                GL_DEPTH_BUFFER_BIT,
                GL_NEAREST
        );

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glEnable(GL_DEPTH_TEST);
        glDepthMask(false);

        shader.useProgram();

        ctx.gBuffer().bindTexture(GBuffer.Attachment.ALBEDO, 0);
        ctx.gBuffer().bindDepthTexture(1);
        shader.setUniform("uAlbedo", 0);
        shader.setUniform("uDepth", 1);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);

        glDepthMask(true);
        shader.unbind();
    }


    @Override
    public void cleanup() {
        shader.destroy();
        glDeleteVertexArrays(vao);
    }
}
