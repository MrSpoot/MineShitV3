package com.mineshit.game.world;

import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.game.utils.GenerationEngine;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class World {
    private static final Logger LOGGER = LoggerFactory.getLogger(World.class);
    private static final int RENDER_DISTANCE = 8; // Change à ta guise

    private final ExecutorService executor = Executors.newFixedThreadPool(4); // 4 threads de génération
    private final Queue<Chunk> chunksReadyToInsert = new ConcurrentLinkedQueue<>();

    private final Map<Vector3i, Chunk> chunks = new HashMap<>();

    public Set<Chunk> getChunks(ChunkState... states) {
        return chunks.values().stream()
                .filter(chunk -> Set.of(states).contains(chunk.getState()))
                .collect(Collectors.toSet());
    }


    public Map<FaceDirection, Chunk> getNeighborChunks(Vector3i position) {
        Map<FaceDirection, Chunk> neighbors = new EnumMap<>(FaceDirection.class);

        for (FaceDirection dir : FaceDirection.values()) {
            Vector3i neighborPos = new Vector3i(position).add(dir.getOffset());
            Chunk neighbor = chunks.get(neighborPos);
            if (neighbor != null) {
                neighbors.put(dir, neighbor);
            }
        }

        return neighbors;
    }


    public void update(Vector3f cameraPosition) {
        generateNewChunk(cameraPosition);
        removeFarChunks(cameraPosition);
    }

    private Vector3i getChunkCameraPosition(Vector3f cameraPosition) {
        int camChunkX = (int) Math.floor(cameraPosition.x / Chunk.SIZE);
        int camChunkY = (int) Math.floor(cameraPosition.y / Chunk.SIZE);
        int camChunkZ = (int) Math.floor(cameraPosition.z / Chunk.SIZE);

        return new Vector3i(camChunkX, camChunkY, camChunkZ);
    }

    private int getSquaredRenderDistance(){
        return RENDER_DISTANCE * RENDER_DISTANCE;
    }

    private void generateNewChunk(Vector3f cameraPosition) {
        Vector3i chunkCameraPosition = getChunkCameraPosition(cameraPosition);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dy = -RENDER_DISTANCE; dy <= RENDER_DISTANCE; dy++) {
                for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                    int distSq = dx * dx + dy * dy + dz * dz;

                    if (distSq > getSquaredRenderDistance()) continue;

                    Vector3i pos = new Vector3i(chunkCameraPosition.x + dx, chunkCameraPosition.y + dy, chunkCameraPosition.z + dz);
                    if (!chunks.containsKey(pos)) {
                        long startTime = System.nanoTime();
                        Chunk chunk = new Chunk(pos);
                        GenerationEngine.generateChunkData(chunk);
                        chunk.setState(ChunkState.GENERATED);

                        long endTime = System.nanoTime();
                        double durationMs = (endTime - startTime) / 1_000_000.0;

                        LOGGER.trace("[GEN] Chunk {} generated in {} ms", pos, String.format("%.3f", durationMs));

                        //generateTestChunk(chunk);
                        chunks.put(pos, chunk);

                        getNeighborChunks(pos).values().forEach(c -> {
                            if (c.getState().equals(ChunkState.MESHED)) c.setState(ChunkState.DIRTY);
                        });
                    }
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
            Chunk chunk = entry.getValue();

            int dx = chunkPos.x - chunkCameraPosition.x;
            int dy = chunkPos.y - chunkCameraPosition.y;
            int dz = chunkPos.z - chunkCameraPosition.z;
            int distSq = dx * dx+ dy * dy + dz * dz;

            if (distSq > getSquaredRenderDistance()) {
                chunk.setState(ChunkState.DELETED);
                iterator.remove();
            }
        }
    }

    private void generateTestChunk(Chunk c1) {

        c1.fillChunk(BlockType.DIRT);

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                c1.setBlock(x, Chunk.SIZE - 1, y, BlockType.GRASS);
            }
        }

        c1.setBlock(0, Chunk.SIZE - 1, 0, BlockType.STONE);
        c1.setBlock(Chunk.SIZE - 1, Chunk.SIZE - 1, 0, BlockType.STONE);
        c1.setBlock(Chunk.SIZE - 1, Chunk.SIZE - 1, Chunk.SIZE - 1, BlockType.STONE);
        c1.setBlock(0, Chunk.SIZE - 1, Chunk.SIZE - 1, BlockType.STONE);

        if (c1.getPosition().x == 0 && c1.getPosition().z == 0) {
            c1.setBlock(0, Chunk.SIZE - 1, 0, BlockType.AIR);
        }

        c1.setState(ChunkState.GENERATED);
    }


}
