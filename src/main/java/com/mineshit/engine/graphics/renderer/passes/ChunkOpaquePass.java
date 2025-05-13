package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.engine.window.Window;
import com.mineshit.game.world.utils.ChunkRenderable;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.opengl.GL11C.*;

public class ChunkOpaquePass implements RenderPass {

    private Shader shader;

    public void init(Window window) {
        this.shader = new Shader("/shaders/opaque_pass.glsl");
    }

    @Override
    public void render(RenderContext ctx) {
        ctx.gbuffer().bind();

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDisable(GL_BLEND);

        shader.useProgram();
        TextureManager.BLOCK_TEXTURES.bind(0);

        shader.setUniform("uProjection", ctx.camera().getProjectionMatrix());
        shader.setUniform("uView", ctx.camera().getViewMatrix());

        Matrix4f viewProj = new Matrix4f(ctx.camera().getProjectionMatrix()).mul(ctx.camera().getViewMatrix());

        for (ChunkRenderable renderable : ChunkRenderable.getRenderableChunksFilterByFrustum(ctx.renderables(),viewProj)) {
            renderable.renderOpaque(ctx.world(), shader);
        }

        shader.unbind();
        ctx.gbuffer().unbind(ctx.window().getWidth(), ctx.window().getHeight());
    }


    public void cleanup(){
        this.shader.destroy();
    }
}
