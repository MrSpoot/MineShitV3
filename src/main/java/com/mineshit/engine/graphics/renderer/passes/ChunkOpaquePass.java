package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.game.world.utils.ChunkRenderable;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkOpaquePass implements RenderPass {

    private Shader shader;

    public void init(){
        this.shader = new Shader("/shaders/basic.glsl");
    }

    @Override
    public void render(RenderContext ctx) {
        shader.useProgram();
        TextureManager.BLOCK_TEXTURES.bind(0);

        shader.setUniform("uProjection", ctx.camera().getProjectionMatrix());
        shader.setUniform("uView", ctx.camera().getViewMatrix());
        shader.setUniform("uLightSpaceMatrix", ctx.lightMatrix());
        shader.setUniform("uShadowMap", 1);
        ctx.shadowMap().bindTexture(1);

        for (ChunkRenderable renderable : ctx.renderables()) {
            renderable.renderOpaque(ctx.world(), shader);
        }

        shader.unbind();
    }

    public void cleanup(){
        this.shader.destroy();
    }
}
