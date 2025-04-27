package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.FrameBuffer;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.engine.window.Window;
import com.mineshit.game.world.utils.ChunkRenderable;

public class ChunkOpaquePass implements RenderPass {

    private Shader shader;
    private FrameBuffer frameBuffer;

    public void init(Window window) {
        this.shader = new Shader("/shaders/opaque_pass.glsl");
        this.frameBuffer = new FrameBuffer(window.getWidth(), window.getHeight());
    }

    @Override
    public void render(RenderContext ctx) {
        frameBuffer.bind();

        shader.useProgram();
        TextureManager.BLOCK_TEXTURES.bind(0);

        shader.setUniform("uProjection", ctx.camera().getProjectionMatrix());
        shader.setUniform("uView", ctx.camera().getViewMatrix());

        for (ChunkRenderable renderable : ctx.renderables()) {
            renderable.renderOpaque(ctx.world(), shader);
        }

        shader.unbind();

        frameBuffer.unbind(ctx.window().getWidth(), ctx.window().getHeight());
    }


    public void cleanup(){
        this.shader.destroy();
        this.frameBuffer.cleanup();
    }
}
