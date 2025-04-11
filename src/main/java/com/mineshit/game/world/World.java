package com.mineshit.game.world;

import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.game.utils.GenerationEngine;
import com.mineshit.game.world.generation.Chunk;
import com.mineshit.game.world.generation.ChunkState;
import com.mineshit.game.world.generation.WorldGeneration;
import com.mineshit.game.world.interaction.WorldInteraction;
import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class World {
    private static final Logger LOGGER = LoggerFactory.getLogger(World.class);
    public static final float CYCLE_DURATION_SECONDS = 20 * 60;

    @Getter
    private final WorldClock clock = new WorldClock(CYCLE_DURATION_SECONDS, 0.5f);
    @Getter
    private final WorldInteraction interaction = new WorldInteraction();

    private final Map<Vector3i, Chunk> chunks = new ConcurrentHashMap<>();
    private final WorldGeneration generation = new WorldGeneration(chunks);

    public Set<Chunk> getChunks(ChunkState... states) {
        return chunks.values().stream()
                .filter(Objects::nonNull)
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

    public Chunk getChunkAt(Vector3f worldPos) {
        int chunkX = (int) Math.floor(worldPos.x / Chunk.SIZE);
        int chunkY = (int) Math.floor(worldPos.y / Chunk.SIZE);
        int chunkZ = (int) Math.floor(worldPos.z / Chunk.SIZE);
        return chunks.get(new Vector3i(chunkX, chunkY, chunkZ));
    }

    public void setDirtyNeighborBlock(Chunk chunk, int localX, int localY, int localZ) {
        Vector3i chunkPos = chunk.getPosition();

        for (FaceDirection dir : FaceDirection.values()) {
            int nx = localX + dir.getOffsetX();
            int ny = localY + dir.getOffsetY();
            int nz = localZ + dir.getOffsetZ();

            if (chunk.isOutOfBounds(nx, ny, nz)) {
                Vector3i neighborPos = new Vector3i(chunkPos).add(dir.getOffset());
                Chunk neighbor = chunks.get(neighborPos);

                if (neighbor != null && neighbor.getState() == ChunkState.MESHED) {
                    neighbor.setState(ChunkState.DIRTY);
                }
            }
        }
    }



    public void update(Vector3f cameraPosition) {
        generation.update(cameraPosition);
    }

    public void cleanup(){
        this.generation.cleanup();
    }
}

