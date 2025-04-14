package com.mineshit.game.world.generation;

import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.game.world.utils.Chunk;
import com.mineshit.game.world.utils.ChunkState;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorldGeneration {

    private static final int RENDER_DISTANCE = 8;

    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final Queue<Chunk> generatedChunks = new ConcurrentLinkedQueue<>();

    private final Map<Vector3i, Chunk> chunks;

    public WorldGeneration(Map<Vector3i, Chunk> chunks) {
        this.chunks = chunks;
    }

    public void update(Vector3f cameraPosition) {
        removeFarChunks(cameraPosition);
        generateNewChunks(cameraPosition);
        flushGeneratedChunks();
    }

    private Vector3i getChunkCameraPosition(Vector3f cameraPosition) {
        int camChunkX = (int) Math.floor(cameraPosition.x / Chunk.SIZE);
        int camChunkY = (int) Math.floor(cameraPosition.y / Chunk.SIZE);
        int camChunkZ = (int) Math.floor(cameraPosition.z / Chunk.SIZE);

        return new Vector3i(camChunkX, camChunkY, camChunkZ);
    }

    private int getSquaredRenderDistance() {
        return RENDER_DISTANCE * RENDER_DISTANCE;
    }

    private void generateNewChunks(Vector3f cameraPosition) {
        Vector3i chunkCameraPosition = getChunkCameraPosition(cameraPosition);

        List<Vector3i> chunksToGenerate = new ArrayList<>();

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dy = -RENDER_DISTANCE; dy <= RENDER_DISTANCE; dy++) {
                for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > getSquaredRenderDistance()) continue;

                    Vector3i pos = new Vector3i(chunkCameraPosition.x + dx, chunkCameraPosition.y + dy, chunkCameraPosition.z + dz);
                    if (chunks.containsKey(pos)) continue;

                    chunksToGenerate.add(pos);
                }
            }
        }

        chunksToGenerate.sort(Comparator.comparingLong(pos ->
                pos.distanceSquared(chunkCameraPosition)
        ));

        for (Vector3i pos : chunksToGenerate) {
            Chunk placeholder = new Chunk(pos);
            placeholder.setState(ChunkState.EMPTY);
            chunks.put(pos, placeholder);

            executor.submit(() -> {
                GenerationEngine.generateChunkData(placeholder);
                placeholder.setState(ChunkState.GENERATED);
                generatedChunks.add(placeholder);
            });
        }
    }



    private void removeFarChunks(Vector3f cameraPosition) {
        Vector3i chunkCameraPosition = getChunkCameraPosition(cameraPosition);

        Iterator<Map.Entry<Vector3i, Chunk>> iterator = chunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Vector3i, Chunk> entry = iterator.next();
            Vector3i chunkPos = entry.getKey();

            int dx = chunkPos.x - chunkCameraPosition.x;
            int dy = chunkPos.y - chunkCameraPosition.y;
            int dz = chunkPos.z - chunkCameraPosition.z;
            int distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > getSquaredRenderDistance()) {
                Chunk chunk = entry.getValue();
                if (chunk != null) {
                    chunk.setState(ChunkState.DELETED);
                }
                iterator.remove();
            }
        }
    }

    private void flushGeneratedChunks() {
        Chunk chunk;
        while ((chunk = generatedChunks.poll()) != null) {
            Vector3i pos = chunk.getPosition();

            for (FaceDirection dir : FaceDirection.values()) {
                Vector3i neighborPos = new Vector3i(pos).add(dir.getOffset());
                Chunk neighbor = chunks.get(neighborPos);
                if (neighbor != null) {
                    neighbor.setState(ChunkState.DIRTY);
                }
            }
        }
    }

    public void cleanup() {
        executor.shutdownNow();
    }

}