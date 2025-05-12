package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.GBuffer;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.window.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL30C.*;

public class LightingPass implements RenderPass {
    private static final Logger LOGGER = LoggerFactory.getLogger(LightingPass.class);

    private Shader shader;
    private int vao;

    @Override
    public void init(Window window) {
        this.shader = new Shader("/shaders/lighting_pass.glsl");
        vao = glGenVertexArrays();
    }

    @Override
    public void render(RenderContext ctx) {
        ctx.lightingMap().bind();

        shader.useProgram();

        ctx.gbuffer().bindTexture(GBuffer.Attachment.ALBEDO, 0);
        ctx.gbuffer().bindTexture(GBuffer.Attachment.NORMAL, 1);
        ctx.gbuffer().bindTexture(GBuffer.Attachment.POSITION, 2);
        ctx.ssaoMap().getTexture().bind(3);

        ctx.shadowMap().bindTexture(4);

        shader.setUniform("uAlbedo", 0);
        shader.setUniform("uNormal", 1);
        shader.setUniform("uPosition", 2);
        shader.setUniform("uSSAO", 3);
        shader.setUniform("uShadow", 4);

        shader.setUniform("uLightSpaceMatrix", ctx.lightMatrix());
        shader.setUniform("uSunDir", ctx.world().getClock().getSunDirection());

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);

        shader.unbind();
        ctx.lightingMap().unbind(ctx.window().getWidth(), ctx.window().getHeight());
    }

    @Override
    public void cleanup() {
        this.shader.destroy();
        glDeleteVertexArrays(vao);
    }
}
