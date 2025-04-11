package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.engine.input.InputManager;
import com.mineshit.engine.utils.Statistic;
import com.mineshit.game.world.generation.Chunk;
import com.mineshit.game.world.generation.ChunkRenderable;
import com.mineshit.game.world.generation.ChunkState;
import com.mineshit.game.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChunkRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkRenderer.class);

    private Shader shader;

    private final Map<Vector3i, ChunkRenderable> renderables = new HashMap<>();

    public void init() {
        LOGGER.info("Initializing ChunkRenderer");
        shader = new Shader("/shaders/basic.glsl");

    }

    public void render(InputManager input, Camera camera, World world, float alpha) {
        world.getInteraction().update(input, world, camera);

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
            renderables.putIfAbsent(pos, new ChunkRenderable(chunk));
        }

        List<Map.Entry<Vector3i, ChunkRenderable>> sorted = new ArrayList<>(renderables.entrySet());

        sorted.sort(Comparator.comparingDouble(entry -> {
            Vector3i pos = entry.getKey();
            float cx = pos.x * Chunk.SIZE;
            float cy = pos.y * Chunk.SIZE;
            float cz = pos.z * Chunk.SIZE;
            return camera.getPosition().distanceSquared(new Vector3f(cx, cy, cz));
        }));

        for (var entry : sorted) {
            ChunkRenderable renderable = entry.getValue();
            renderable.updateMeshIfNeeded(world);
        }

        for (ChunkRenderable renderable : renderables.values()) {
            renderable.render(world,shader);
        }

        TextureManager.BLOCK_TEXTURES.unbind();
        shader.unbind();
    }

    public void cleanup() {
        shader.destroy();
        for (ChunkRenderable renderable : renderables.values()) {
            renderable.cleanup();
        }
        ChunkRenderable.cleanupStatic();
    }
}
