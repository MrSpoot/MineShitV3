package com.mineshit.game.world.generation;

import com.mineshit.game.utils.FastNoiseLite;
import com.mineshit.game.world.utils.BlockType;
import com.mineshit.game.world.utils.Chunk;

public class GenerationEngine {

    private static final long SEED = 1;
    private static final float AMPLITUDE = 25.0f;
    private static final int BASE_HEIGHT = 0;
    private static final int WATER_LEVEL = 0;

    private static final FastNoiseLite noise = new FastNoiseLite();
    private static final FastNoiseLite tempNoise = new FastNoiseLite();
    private static final FastNoiseLite humNoise = new FastNoiseLite();
    private static final FastNoiseLite treeNoise = new FastNoiseLite();

    static {
        noise.SetSeed((int) SEED);
        tempNoise.SetSeed((int) SEED + 123);
        humNoise.SetSeed((int) SEED + 456);
        treeNoise.SetSeed((int) SEED + 999);
    }

    public static void generateChunkData(Chunk chunk) {

        short[] data = new short[Chunk.TOTAL_BLOCKS];

        int chunkGlobalX = chunk.getPosition().x * Chunk.SIZE;
        int chunkGlobalZ = chunk.getPosition().z * Chunk.SIZE;
        int chunkGlobalY = chunk.getPosition().y * Chunk.SIZE;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int globalX = chunkGlobalX + x;
                int globalZ = chunkGlobalZ + z;

                float height = getFractalNoise(noise, globalX * 0.3f, globalZ * 0.3f, 4, 2.0f, 0.5f) * AMPLITUDE + BASE_HEIGHT;
                int heightInt = (int) height;

                for (int y = 0; y < Chunk.SIZE; y++) {
                    int globalY = chunkGlobalY + y;
                    BiomeType biome = getBiomeAt(globalX, globalZ);

                    if (globalY > Math.max(heightInt, WATER_LEVEL)) {
                        chunk.setBlock(x, y, z, BlockType.AIR);
                    } else if (globalY > heightInt && globalY <= WATER_LEVEL) {
                        chunk.setBlock(x, y, z, BlockType.WATER);
                    } else if (globalY == heightInt) {
                        switch (biome) {
                            case DESERT -> chunk.setBlock(x, y, z, BlockType.SAND);
                            case MOUNTAIN -> chunk.setBlock(x, y, z, BlockType.STONE);
                            case OCEAN -> chunk.setBlock(x, y, z, BlockType.WATER);
                            case PLAIN -> chunk.setBlock(x, y, z, BlockType.GRASS);
                        }
                    } else if (globalY >= heightInt - 3) {
                        chunk.setBlock(x, y, z, BlockType.DIRT);
                    } else {
                        chunk.setBlock(x, y, z, BlockType.STONE);
                    }
                }
            }
        }

        for (int x = 2; x < Chunk.SIZE - 2; x++) {
            for (int z = 2; z < Chunk.SIZE - 2; z++) {
                int globalX = chunkGlobalX + x;
                int globalZ = chunkGlobalZ + z;

                int surfaceY = getSurfaceY(chunk, x, z);
                if (surfaceY == -1) continue;

                if (chunk.getBlock(x, surfaceY, z) == BlockType.GRASS.getId()) {
                    float chance = treeNoise.GetNoise(globalX, globalZ);
                    if (chance > 0.85f) {
                        tryPlaceTree(chunk, x, surfaceY + 1, z);
                    }
                }
            }
        }
    }

    private static int getSurfaceY(Chunk chunk, int x, int z) {
        for (int y = Chunk.SIZE - 1; y >= 0; y--) {
            if (chunk.getBlock(x, y, z) != BlockType.AIR.getId()) {
                return y;
            }
        }
        return -1;
    }

    private static void tryPlaceTree(Chunk chunk, int x, int y, int z) {
        if (!chunk.isInBounds(x, y + 5, z)) return;

        for (int i = 0; i < 4; i++) {
            chunk.setBlock(x, y + i, z, BlockType.WOOD_LOG);
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int dist = dx * dx + dy * dy + dz * dz;
                    if (dist <= 5) {
                        int px = x + dx;
                        int py = y + 3 + dy;
                        int pz = z + dz;
                        if (chunk.isInBounds(px, py, pz) && chunk.getBlock(px, py, pz) == BlockType.AIR.getId()) {
                            chunk.setBlock(px, py, pz, BlockType.LEAVE);
                        }
                    }
                }
            }
        }
    }

    public static BiomeType getBiomeAt(int x, int z) {
        float temperature = getFractalNoise(tempNoise, x * 0.01f, z * 0.01f, 4, 2.0f, 0.5f);
        float humidity = getFractalNoise(humNoise, x * 0.01f, z * 0.01f, 4, 2.0f, 0.5f);
        return BiomeType.fromClimate(temperature, humidity);
    }

    public static float getFractalNoise(FastNoiseLite noise, float x, float y, int octaves, float lacunarity, float gain) {
        float total = 0f;
        float frequency = 1f;
        float amplitude = 1f;
        float maxAmplitude = 0f;

        for (int i = 0; i < octaves; i++) {
            total += noise.GetNoise(x * frequency, y * frequency) * amplitude;
            maxAmplitude += amplitude;
            frequency *= lacunarity;
            amplitude *= gain;
        }

        return total / maxAmplitude;
    }
}
