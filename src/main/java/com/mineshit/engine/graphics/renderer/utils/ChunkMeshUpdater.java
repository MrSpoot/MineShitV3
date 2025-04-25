package com.mineshit.engine.graphics.renderer.utils;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.game.world.World;
import com.mineshit.game.world.utils.Chunk;
import com.mineshit.game.world.utils.ChunkRenderable;
import com.mineshit.game.world.utils.ChunkState;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChunkMeshUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkMeshUpdater.class);

    public static void update(Map<Vector3i, ChunkRenderable> renderables, World world, Camera camera) {
        Iterator<Map.Entry<Vector3i, ChunkRenderable>> it = renderables.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ChunkRenderable renderable = entry.getValue();

            if (renderable.getChunk().getState() == ChunkState.DELETED) {
                renderable.cleanup();
                it.remove();
            }
        }

        for (Chunk chunk : world.getChunks(ChunkState.GENERATED, ChunkState.DIRTY, ChunkState.DIRTY_NOW)) {
            Vector3i pos = chunk.getPosition();
            renderables.putIfAbsent(pos, new ChunkRenderable(chunk));
        }

        for(ChunkRenderable renderable : renderables.values().stream().filter(cr -> cr.getChunk().getState().equals(ChunkState.DIRTY_NOW)).toList()) {
            renderable.forceRebuild(world);

            for (FaceDirection dir : FaceDirection.values()) {
                Vector3i neighborChunkPos = new Vector3i(renderable.getChunk().getPosition()).add(dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ());
                ChunkRenderable neighborChunk = renderables.get(neighborChunkPos);
                if(neighborChunk != null) {
                    neighborChunk.forceRebuild(world);
                }
            }
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
    }
}
