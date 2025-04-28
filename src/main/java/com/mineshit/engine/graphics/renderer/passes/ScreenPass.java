package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.FrameBuffer;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.window.Window;

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

        LightingPass lightingPass = ctx.getPass(LightingPass.class);
        ChunkOpaquePass opaquePass = ctx.getPass(ChunkOpaquePass.class);
        ChunkTransparentPass transparentPass = ctx.getPass(ChunkTransparentPass.class);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, lightingPass.getFrameBuffer().getFbo());
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBlitFramebuffer(
                0, 0, lightingPass.getFrameBuffer().getWidth(), lightingPass.getFrameBuffer().getHeight(),
                0, 0, lightingPass.getFrameBuffer().getWidth(), lightingPass.getFrameBuffer().getHeight(),
                GL_DEPTH_BUFFER_BIT, GL_NEAREST
        );
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glEnable(GL_DEPTH_TEST);
        glDepthMask(false);

        shader.useProgram();

        // Bind textures
        FrameBuffer.bindTexture(lightingPass.getFrameBuffer().getAlbedoTexture(), 0);
        FrameBuffer.bindTexture(transparentPass.getFrameBuffer().getAlbedoTexture(), 1);
        FrameBuffer.bindTexture(lightingPass.getFrameBuffer().getDepthTexture(), 2);
        FrameBuffer.bindTexture(transparentPass.getFrameBuffer().getDepthTexture(), 3);

        shader.setUniform("uOpaqueColor", 0);
        shader.setUniform("uTransparentColor", 1);
        shader.setUniform("uOpaqueDepth", 2);
        shader.setUniform("uTransparentDepth", 3);

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
