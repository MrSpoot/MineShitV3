package com.mineshit.game.utils;

import com.mineshit.game.world.BlockType;
import com.mineshit.game.world.Chunk;
public class GenerationEngine {

    private static final long SEED = 1;
    private static final float AMPLITUDE = 50.0f;
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

                // Calcul de la hauteur en (x,z)
                float height = noise.GetNoise(globalX * 0.5f, globalZ * 0.5f) * AMPLITUDE;
                int heightInt = (int) height;

                for (int y = 0; y < Chunk.SIZE; y++) {
                    int globalY = chunkGlobalY + y;

                    if (globalY > heightInt) {
                        chunk.setBlock(x, y, z, BlockType.AIR);
                    } else if (globalY == heightInt) {
                        chunk.setBlock(x, y, z, BlockType.GRASS);
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
