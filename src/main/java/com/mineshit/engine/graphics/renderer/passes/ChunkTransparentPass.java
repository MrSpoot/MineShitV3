package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.FrameBuffer;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.engine.window.Window;
import com.mineshit.game.world.utils.Chunk;
import com.mineshit.game.world.utils.ChunkRenderable;
import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.glDepthMask;
import static org.lwjgl.opengl.GL11C.glDisable;

public class ChunkTransparentPass implements RenderPass {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkTransparentPass.class);

    private Shader shader;

    @Getter
    private FrameBuffer frameBuffer;

    @Override
    public void init(Window window) {
        this.shader = new Shader("/shaders/transparent_pass.glsl");
        this.frameBuffer = new FrameBuffer(window.getWidth(), window.getHeight());
    }

    @Override
    public void render(RenderContext ctx) {
        frameBuffer.bind();

        shader.useProgram();

        TextureManager.BLOCK_TEXTURES.bind(0);

        shader.setUniform("uProjection", ctx.camera().getProjectionMatrix());
        shader.setUniform("uView", ctx.camera().getViewMatrix());

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //glDepthMask(false);

        Collection<ChunkRenderable> sortedTransparent = ctx.renderables()
                .stream()
                .filter(ChunkRenderable::hasTransparent)
                .sorted(Comparator.comparingDouble(cr -> {
                    float cx = cr.getChunk().getPosition().x * Chunk.SIZE;
                    float cy = cr.getChunk().getPosition().y * Chunk.SIZE;
                    float cz = cr.getChunk().getPosition().z * Chunk.SIZE;
                    return -ctx.camera().getPosition().distanceSquared(new Vector3f(cx, cy, cz));
                }))
                .toList();


        for (ChunkRenderable cr : sortedTransparent) {
            cr.renderTransparent(ctx.world(), shader);
        }

        //glDepthMask(true);
        glDisable(GL_BLEND);

        frameBuffer.unbind(ctx.window().getWidth(), ctx.window().getHeight());
    }

    @Override
    public void cleanup() {
        this.shader.destroy();
    }
}
