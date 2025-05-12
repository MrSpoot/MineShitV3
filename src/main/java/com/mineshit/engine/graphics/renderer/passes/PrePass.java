package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.GBuffer;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.engine.window.Window;
import com.mineshit.game.world.utils.ChunkRenderable;
import org.joml.Vector2f;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL30C.*;

public class PrePass implements RenderPass{

    private int vao;
    private Shader shadowShader;
    private Shader ssaoShader;

    @Override
    public void init(Window window) {
        this.vao = glGenVertexArrays();
        this.shadowShader = new Shader("/shaders/shadow.glsl");
        this.ssaoShader = new Shader("/shaders/ssao.glsl");
    }

    @Override
    public void render(RenderContext ctx) {
        shadowMap(ctx);
        ssao(ctx);
    }

    @Override
    public void cleanup() {
        shadowShader.destroy();
        ssaoShader.destroy();
        glDeleteVertexArrays(vao);
    }

    private void shadowMap(RenderContext ctx){
        glDisable(GL_CULL_FACE);

        ctx.shadowMap().bind();

        shadowShader.useProgram();

        TextureManager.BLOCK_TEXTURES.bind(0);
        shadowShader.setUniform("uTextureArray", 0);
        shadowShader.setUniform("uLightSpaceMatrix", ctx.lightMatrix());

        for (ChunkRenderable renderable : ctx.renderables()) {
            renderable.renderShadow(ctx.world(), shadowShader, ctx.lightMatrix());
        }

        shadowShader.unbind();
        ctx.shadowMap().unbind(ctx.window().getWidth(), ctx.window().getHeight());
        glEnable(GL_CULL_FACE);
    }

    private void ssao(RenderContext ctx){
        ssaoShader.useProgram();

        ctx.ssaoMap().bind();

        ctx.gbuffer().bindTexture(GBuffer.Attachment.NORMAL,0);
        ctx.gbuffer().bindTexture(GBuffer.Attachment.POSITION,1);

        ssaoShader.setUniform("uNormal", 0);
        ssaoShader.setUniform("uPosition", 1);
        ssaoShader.setUniform("uCameraPos", ctx.camera().getPosition());

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);

        ctx.ssaoMap().unbind(ctx.window().getWidth(), ctx.window().getHeight());

        ssaoShader.unbind();
    }
}
