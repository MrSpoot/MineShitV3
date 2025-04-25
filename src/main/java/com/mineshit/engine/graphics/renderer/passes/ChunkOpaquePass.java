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
        this.shader = new Shader("/shaders/opaque_pass.glsl");
    }

    @Override
    public void render(RenderContext ctx) {
        ctx.gBuffer().bind();

        shader.useProgram();
        TextureManager.BLOCK_TEXTURES.bind(0);

        shader.setUniform("uProjection", ctx.camera().getProjectionMatrix());
        shader.setUniform("uView", ctx.camera().getViewMatrix());

        for (ChunkRenderable renderable : ctx.renderables()) {
            renderable.renderOpaque(ctx.world(), shader);
        }

        shader.unbind();
        ctx.gBuffer().unbind(ctx.window().getWidth(), ctx.window().getHeight());
    }


    public void cleanup(){
        this.shader.destroy();
    }
}
