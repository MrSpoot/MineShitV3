package com.mineshit.game.world;

import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.stream.Collectors;

public class World {
    private final Map<Vector3i, Chunk> chunks = new HashMap<>();
    private final Map<Vector3i, ChunkRenderable> renderables = new HashMap<>();

    public Collection<ChunkRenderable> getRenderables() {
        return renderables.values();
    }

    public Set<Chunk> getChunks(ChunkState state) {
        return chunks.values().stream().filter(chunk -> chunk.getState() == state).collect(Collectors.toSet());
    }

    public void update(Vector3f cameraPosition) {
        generateNewChunk(cameraPosition);
    }

    private void generateNewChunk(Vector3f cameraPosition) {
        if (chunks.isEmpty()) {
            for (Vector3i pos : List.of(
                    new Vector3i(0, 0, 0),
                    new Vector3i(1, 0, 0),
                    new Vector3i(0, 0, 1),
                    new Vector3i(-1, 0, 0),
                    new Vector3i(0, 0, -1)
            )) {
                Chunk chunk = generateTestChunk(pos);
                chunks.put(pos, chunk);
                renderables.put(pos, new ChunkRenderable(chunk));
            }
        }
    }

    private Chunk generateTestChunk(Vector3i position) {
            Chunk c1 = new Chunk(position);
            c1.fillChunk((short)1);
            c1.setBlock(0,Chunk.SIZE - 1,0,(short)2);
            c1.setBlock(Chunk.SIZE - 1,Chunk.SIZE - 1,0,(short)2);
            c1.setBlock(Chunk.SIZE - 1,Chunk.SIZE - 1,Chunk.SIZE - 1,(short)2);
            c1.setBlock(0,Chunk.SIZE - 1,Chunk.SIZE - 1,(short)2);

            if(position.x == 0 && position.z == 0) {
                c1.setBlock(Chunk.SIZE / 2,Chunk.SIZE - 1,Chunk.SIZE / 2,(short)2);
                c1.setBlock(Chunk.SIZE / 2 + 1,Chunk.SIZE - 1,Chunk.SIZE / 2,(short)2);
                c1.setBlock(Chunk.SIZE / 2 + 1,Chunk.SIZE - 1,Chunk.SIZE / 2 + 1,(short)2);
                c1.setBlock(Chunk.SIZE / 2,Chunk.SIZE - 1,Chunk.SIZE / 2 + 1,(short)2);
            }

            c1.setBlock(0,0,0,(short)2);
            c1.setBlock(Chunk.SIZE - 1,0,0,(short)2);
            c1.setBlock(Chunk.SIZE - 1,0,Chunk.SIZE - 1,(short)2);
            c1.setBlock(0,0,Chunk.SIZE - 1,(short)2);

            c1.setState(ChunkState.GENERATED);
            return c1;
    }


}
