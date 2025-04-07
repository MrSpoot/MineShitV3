package com.mineshit.game.world.generation;

import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.game.utils.GenerationEngine;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorldGeneration {

    private static final int RENDER_DISTANCE = 8;

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Queue<Chunk> generatedChunks = new ConcurrentLinkedQueue<>();

    private final Map<Vector3i, Chunk> chunks;

    public WorldGeneration(Map<Vector3i, Chunk> chunks) {
        this.chunks = chunks;
    }

    public void update(Vector3f cameraPosition) {
        generateNewChunks(cameraPosition);
        removeFarChunks(cameraPosition);
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

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dy = -RENDER_DISTANCE; dy <= RENDER_DISTANCE; dy++) {
                for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > getSquaredRenderDistance()) continue;

                    Vector3i pos = new Vector3i(chunkCameraPosition.x + dx, chunkCameraPosition.y + dy, chunkCameraPosition.z + dz);

                    Chunk placeholder = new Chunk(pos);
                    placeholder.setState(ChunkState.EMPTY);

                    if (chunks.putIfAbsent(pos, placeholder) != null) continue;

                    executor.submit(() -> {
                        GenerationEngine.generateChunkData(placeholder);
                        placeholder.setState(ChunkState.GENERATED);
                        generatedChunks.add(placeholder);
                    });

                    executor.submit(() -> {
                        Chunk chunk = new Chunk(pos);
                        GenerationEngine.generateChunkData(chunk);
                        chunk.setState(ChunkState.GENERATED);
                        generatedChunks.add(chunk);
                    });
                }
            }
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
                if (neighbor != null && neighbor.getState() == ChunkState.MESHED) {
                    neighbor.setState(ChunkState.DIRTY);
                }
            }
        }
    }

    public void cleanup() {
        executor.shutdownNow();
    }

}