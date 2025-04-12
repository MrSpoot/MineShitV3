package com.mineshit.game.utils;

import com.mineshit.game.world.generation.BlockType;
import com.mineshit.game.world.generation.Chunk;

public class GenerationEngine {

    private static final long SEED = 1;
    private static final float AMPLITUDE = 25.0f;
    private static final int BASE_HEIGHT = 10;
    private static final int WATER_LEVEL = -11 ;
    private static final FastNoiseLite noise = new FastNoiseLite();

    static {
        noise.SetSeed((int) SEED);
    }

    public static void generateChunkData(Chunk chunk) {
        int chunkGlobalX = chunk.getPosition().x * Chunk.SIZE;
        int chunkGlobalZ = chunk.getPosition().z * Chunk.SIZE;
        int chunkGlobalY = chunk.getPosition().y * Chunk.SIZE;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int globalX = chunkGlobalX + x;
                int globalZ = chunkGlobalZ + z;

                float height = noise.GetNoise(globalX * 0.3f, globalZ * 0.3f) * AMPLITUDE + BASE_HEIGHT;
                int heightInt = (int) height;

                for (int y = 0; y < Chunk.SIZE; y++) {
                    int globalY = chunkGlobalY + y;

                    if (globalY > Math.max(heightInt, WATER_LEVEL)) {
                        chunk.setBlock(x, y, z, BlockType.AIR);
                    } else if (globalY > heightInt && globalY <= WATER_LEVEL) {
                        chunk.setBlock(x, y, z, BlockType.WATER);
                    } else if (globalY == heightInt) {
                        if (globalY < -10) {
                            chunk.setBlock(x, y, z, BlockType.SAND);
                        } else {
                            chunk.setBlock(x, y, z, BlockType.GRASS);
                        }
                    } else if (globalY >= heightInt - 3) {
                        chunk.setBlock(x, y, z, BlockType.DIRT);
                    } else {
                        chunk.setBlock(x, y, z, BlockType.STONE);
                    }
                }
            }
        }
    }
}
