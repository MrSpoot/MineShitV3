package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.game.world.utils.ChunkRenderable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11C.*;

public class ChunkShadowPass implements RenderPass {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkShadowPass.class);

    private Shader shader;

    @Override
    public void init() {
        this.shader = new Shader("/shaders/shadow.glsl");
    }

    @Override
    public void render(RenderContext ctx) {
        glDisable(GL_CULL_FACE);

        ctx.shadowMap().bind();

        shader.useProgram();

        TextureManager.BLOCK_TEXTURES.bind(0);
        shader.setUniform("uTextureArray", 0);
        shader.setUniform("uLightSpaceMatrix", ctx.lightMatrix());

        for (ChunkRenderable renderable : ctx.renderables()) {
            renderable.renderShadow(ctx.world(), shader, ctx.lightMatrix());
        }

        shader.unbind();
        ctx.shadowMap().unbind(ctx.window().getWidth(), ctx.window().getHeight());
        glEnable(GL_CULL_FACE);
    }

    @Override
    public void cleanup() {
        this.shader.destroy();
    }
}
