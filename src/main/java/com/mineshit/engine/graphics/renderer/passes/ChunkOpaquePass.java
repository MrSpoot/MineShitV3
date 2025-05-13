package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.CrossMesh;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.engine.window.Window;
import com.mineshit.game.world.utils.Chunk;
import com.mineshit.game.world.utils.ChunkRenderable;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL33C.glVertexAttribDivisor;

public class ChunkOpaquePass implements RenderPass {

    private Shader shader;
    private Shader crossShader;

    public void init(Window window) {
        this.shader = new Shader("/shaders/opaque_pass.glsl");
        this.crossShader = new Shader("/shaders/cross_pass.glsl");
        CrossMesh.init();
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

        renderCrossMesh(ctx);

        ctx.gbuffer().unbind(ctx.window().getWidth(), ctx.window().getHeight());
    }

    private void renderCrossMesh(RenderContext ctx){
        Matrix4f viewProj = new Matrix4f(ctx.camera().getProjectionMatrix()).mul(ctx.camera().getViewMatrix());
        Collection<ChunkRenderable> visibleChunks = ChunkRenderable.getRenderableChunksFilterByFrustum(ctx.renderables(),viewProj);

        FloatBuffer crossInstances = MemoryUtil.memAllocFloat(visibleChunks.stream().mapToInt(ChunkRenderable::getCrossInstanceCount).sum() * 4);

        for (ChunkRenderable chunk : visibleChunks) {
            if (chunk.getCrossInstanceCount() == 0) continue;

            FloatBuffer src = chunk.getCrossInstanceBuffer().duplicate();
            src.rewind();

            Vector3f chunkWorldOffset = new Vector3f(
                    chunk.getChunk().getPosition().x * Chunk.SIZE,
                    chunk.getChunk().getPosition().y * Chunk.SIZE,
                    chunk.getChunk().getPosition().z * Chunk.SIZE
            );

            for (int i = 0; i < src.limit(); i += 4) {
                float localX = src.get(i);
                float localY = src.get(i + 1);
                float localZ = src.get(i + 2);
                float texId = src.get(i + 3);

                float worldX = localX + chunkWorldOffset.x;
                float worldY = localY + chunkWorldOffset.y;
                float worldZ = localZ + chunkWorldOffset.z;

                crossInstances.put(worldX);
                crossInstances.put(worldY);
                crossInstances.put(worldZ);
                crossInstances.put(texId);
            }
        }

        crossInstances.flip();

        crossShader.useProgram();

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDisable(GL_BLEND);

        glDisable(GL_CULL_FACE);
        TextureManager.BLOCK_TEXTURES.bind(0);

        crossShader.setUniform("uProjection", ctx.camera().getProjectionMatrix());
        crossShader.setUniform("uView", ctx.camera().getViewMatrix());

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

        glEnable(GL_CULL_FACE);

        crossShader.unbind();
    }


    public void cleanup(){
        this.shader.destroy();
    }
}
