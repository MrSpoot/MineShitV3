package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.FrameBuffer;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.window.Window;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL30C.*;

public class LightingPass implements RenderPass{
    private static final Logger LOGGER = LoggerFactory.getLogger(LightingPass.class);

    private Shader shader;
    @Getter
    private FrameBuffer frameBuffer;
    private int vao;

    @Override
    public void init(Window window) {
        this.shader = new Shader("/shaders/lighting_pass.glsl");
        this.frameBuffer = new FrameBuffer(window.getWidth(), window.getHeight());
        vao = glGenVertexArrays();
    }

    @Override
    public void render(RenderContext ctx) {

        ChunkOpaquePass opaquePass = ctx.getPass(ChunkOpaquePass.class);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, opaquePass.getFrameBuffer().getFbo());
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, frameBuffer.getFbo());

        glBlitFramebuffer(
                0, 0, opaquePass.getFrameBuffer().getWidth(), opaquePass.getFrameBuffer().getHeight(),
                0, 0, frameBuffer.getWidth(), frameBuffer.getHeight(),
                GL_DEPTH_BUFFER_BIT, GL_NEAREST
        );

        frameBuffer.bind();
        shader.useProgram();

        FrameBuffer.bindTexture(opaquePass.getFrameBuffer().getAlbedoTexture(), 0);
        FrameBuffer.bindTexture(opaquePass.getFrameBuffer().getNormalTexture(), 1);
        FrameBuffer.bindTexture(opaquePass.getFrameBuffer().getPositionTexture(), 2);
        FrameBuffer.bindTexture(opaquePass.getFrameBuffer().getDepthTexture(), 3);
        ctx.shadowMap().bindTexture(4);

        shader.setUniform("uAlbedo",0);
        shader.setUniform("uNormal",1);
        shader.setUniform("uPosition",2);
        shader.setUniform("uDepth",3);
        shader.setUniform("uShadow",4);

        shader.setUniform("uLightSpaceMatrix", ctx.lightMatrix());
        shader.setUniform("uSunDir", ctx.world().getClock().getSunDirection());

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);

        shader.unbind();
        frameBuffer.unbind(ctx.window().getWidth(), ctx.window().getHeight());
    }

    @Override
    public void cleanup() {
        this.shader.destroy();
        this.frameBuffer.cleanup();
    }
}
