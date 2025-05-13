package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.CrossMesh;
import com.mineshit.engine.graphics.renderer.utils.GBuffer;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.engine.window.Window;
import com.mineshit.game.world.utils.Chunk;
import com.mineshit.game.world.utils.ChunkRenderable;
import org.joml.FrustumIntersection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL33C.glVertexAttribDivisor;

public class PrePass implements RenderPass{

    private int vao;
    private Shader shadowShader;
    private Shader crossShadowShader;
    private Shader ssaoShader;

    @Override
    public void init(Window window) {
        this.vao = glGenVertexArrays();
        this.shadowShader = new Shader("/shaders/shadow.glsl");
        this.crossShadowShader = new Shader("/shaders/cross_shadow_pass.glsl");
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

        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(4.0f, 8.0f); // Bias stable voxel

        ctx.shadowMap().bind();

        shadowShader.useProgram();

        TextureManager.BLOCK_TEXTURES.bind(0);
        shadowShader.setUniform("uTextureArray", 0);
        shadowShader.setUniform("uLightSpaceMatrix", ctx.lightMatrix());

        for (ChunkRenderable renderable : ChunkRenderable.getRenderableChunksFilterByFrustum(ctx.renderables(),ctx.lightMatrix())) {
            renderable.renderShadow(ctx.world(), shadowShader, ctx.lightMatrix());
        }

        shadowShader.unbind();

        renderCrossShadow(ctx);

        ctx.shadowMap().unbind(ctx.window().getWidth(), ctx.window().getHeight());

        glDisable(GL_POLYGON_OFFSET_FILL);
        glEnable(GL_CULL_FACE);
    }

    private void renderCrossShadow(RenderContext ctx){
        crossShadowShader.useProgram();
        crossShadowShader.setUniform("uLightSpaceMatrix", ctx.lightMatrix());

        Collection<ChunkRenderable> visibleChunks = ChunkRenderable.getRenderableChunksFilterByFrustum(ctx.renderables(), ctx.lightMatrix());
        FloatBuffer crossInstances = MemoryUtil.memAllocFloat(visibleChunks.stream().mapToInt(ChunkRenderable::getCrossInstanceCount).sum() * 4);

        for (ChunkRenderable chunk : visibleChunks) {
            if (chunk.getCrossInstanceCount() == 0) continue;
            FloatBuffer src = chunk.getCrossInstanceBuffer().duplicate();
            src.rewind();
            Vector3f offset = new Vector3f(
                    chunk.getChunk().getPosition().x * Chunk.SIZE,
                    chunk.getChunk().getPosition().y * Chunk.SIZE,
                    chunk.getChunk().getPosition().z * Chunk.SIZE
            );

            for (int i = 0; i < src.limit(); i += 4) {
                float x = src.get(i) + offset.x;
                float y = src.get(i + 1) + offset.y;
                float z = src.get(i + 2) + offset.z;
                float texId = src.get(i + 3);
                crossInstances.put(x).put(y).put(z).put(texId);
            }
        }
        crossInstances.flip();

        CrossMesh.bind();
        int instanceVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
        glBufferData(GL_ARRAY_BUFFER, crossInstances, GL_STATIC_DRAW);

        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glVertexAttribDivisor(2, 1);

        CrossMesh.render(crossInstances.remaining() / 4);

        glDeleteBuffers(instanceVBO);
        MemoryUtil.memFree(crossInstances);

        crossShadowShader.unbind();

        ctx.shadowMap().unbind(ctx.window().getWidth(), ctx.window().getHeight());

        glDisable(GL_POLYGON_OFFSET_FILL);
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
