package com.mineshit.engine.graphics;

import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.game.world.Chunk;
import com.mineshit.game.world.ChunkRenderable;
import com.mineshit.game.world.ChunkState;
import com.mineshit.game.world.World;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class ChunkRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkRenderer.class);

    private Shader shader;

    private final Map<Vector3i, ChunkRenderable> renderables = new HashMap<>();

    public void init() {
        LOGGER.info("Initializing ChunkRenderer");
        shader = new Shader("/shaders/basic.glsl");
    }

    public void render(Camera camera, World world, float alpha) {
        shader.useProgram();
        TextureManager.BLOCK_TEXTURES.bind(0);

        shader.setUniform("uProjection", camera.getProjectionMatrix());
        shader.setUniform("uView", camera.getViewMatrix());

        Iterator<Map.Entry<Vector3i, ChunkRenderable>> it = renderables.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ChunkRenderable renderable = entry.getValue();

            if (renderable.getChunk().getState() == ChunkState.DELETED) {
                renderable.cleanup();
                it.remove();
            }
        }

        for (Chunk chunk : world.getChunks(ChunkState.GENERATED, ChunkState.DIRTY)) {
            Vector3i pos = chunk.getPosition();
            ChunkRenderable renderable = renderables.computeIfAbsent(pos, k -> new ChunkRenderable(chunk));
            renderable.updateMeshIfNeeded(world);
        }

        for(ChunkRenderable renderable : renderables.values()) {
            renderable.render(shader);
        }

        TextureManager.BLOCK_TEXTURES.unbind();
        shader.unbind();
    }

    public void cleanup() {
        shader.destroy();
        for (ChunkRenderable renderable : renderables.values()) {
            renderable.cleanup();
        }
    }
}
