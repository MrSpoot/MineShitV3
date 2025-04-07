package com.mineshit.engine.game;

import com.mineshit.engine.graphics.renderer.Mesh;
import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.game.world.generation.Chunk;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;

import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memAllocInt;

public class ChunkMeshBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkMeshBuilder.class);

    private static final float[][] UVS = {
            {0f, 0f},
            {1f, 0f},
            {1f, 1f},
            {0f, 1f}
    };

    public static ChunkMeshData buildBuffers(Chunk chunk, Map<FaceDirection, Chunk> neighbors) {
        long startTime = System.nanoTime();
        LOGGER.trace("Building Mesh");

        int maxFaces = Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 6;
        FloatBuffer vertexBuffer = memAllocFloat(maxFaces * 4 * 7); // 7 floats per vertex now (added faceId)
        IntBuffer indexBuffer = memAllocInt(maxFaces * 6);

        int indexOffset = 0;
        int vertexCount = 0;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    short block = chunk.getBlock(x, y, z);
                    if (block == 0) continue;

                    for (FaceDirection face : FaceDirection.values()) {
                        int nx = x + face.getOffsetX();
                        int ny = y + face.getOffsetY();
                        int nz = z + face.getOffsetZ();

                        short neighborBlock = 0;

                        if (chunk.isOutOfBounds(nx, ny, nz)) {
                            Chunk neighbor = neighbors.get(face);
                            if (neighbor != null) {
                                int ox = (nx + Chunk.SIZE) % Chunk.SIZE;
                                int oy = (ny + Chunk.SIZE) % Chunk.SIZE;
                                int oz = (nz + Chunk.SIZE) % Chunk.SIZE;
                                if (neighbor.isInBounds(ox, oy, oz)) {
                                    neighborBlock = neighbor.getBlock(ox, oy, oz);
                                }
                            }
                        } else {
                            neighborBlock = chunk.getBlock(nx, ny, nz);
                        }

                        if (neighborBlock != 0) continue; // Face cachée : on la skip

                        // Sinon, on génère la face
                        float[] faceVertices = getFaceVertices(x, y, z, face);
                        int faceId = getFaceId(face);

                        for (int i = 0; i < 4; i++) {
                            vertexBuffer.put(faceVertices[i * 3]);
                            vertexBuffer.put(faceVertices[i * 3 + 1]);
                            vertexBuffer.put(faceVertices[i * 3 + 2]);
                            vertexBuffer.put(UVS[i][0]);
                            vertexBuffer.put(UVS[i][1]);
                            vertexBuffer.put(block);
                            vertexBuffer.put(faceId);
                        }

                        indexBuffer.put(indexOffset);
                        indexBuffer.put(indexOffset + 1);
                        indexBuffer.put(indexOffset + 2);
                        indexBuffer.put(indexOffset + 2);
                        indexBuffer.put(indexOffset + 3);
                        indexBuffer.put(indexOffset);
                        indexOffset += 4;
                        vertexCount += 4;
                    }
                }
            }
        }

        vertexBuffer.flip();
        indexBuffer.flip();

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;

        LOGGER.trace("ChunkMesh built in {} ms ({} vertices, {} triangles)",
                String.format("%.2f", durationMs),
                vertexCount,
                indexOffset / 2
        );

        return new ChunkMeshData(vertexBuffer, indexBuffer, vertexCount);
    }


    private static float[] getFaceVertices(int x, int y, int z, FaceDirection face) {
        float fx = x;
        float fy = y;
        float fz = z;

        return switch (face) {
            case FRONT -> new float[]{ fx, fy, fz + 1, fx + 1, fy, fz + 1, fx + 1, fy + 1, fz + 1, fx, fy + 1, fz + 1 };
            case BACK -> new float[]{ fx + 1, fy, fz, fx, fy, fz, fx, fy + 1, fz, fx + 1, fy + 1, fz };
            case LEFT -> new float[]{ fx, fy, fz, fx, fy, fz + 1, fx, fy + 1, fz + 1, fx, fy + 1, fz };
            case RIGHT -> new float[]{ fx + 1, fy, fz + 1, fx + 1, fy, fz, fx + 1, fy + 1, fz, fx + 1, fy + 1, fz + 1 };
            case TOP -> new float[]{ fx, fy + 1, fz + 1, fx + 1, fy + 1, fz + 1, fx + 1, fy + 1, fz, fx, fy + 1, fz };
            case BOTTOM -> new float[]{ fx, fy, fz, fx + 1, fy, fz, fx + 1, fy, fz + 1, fx, fy, fz + 1 };
        };
    }

    private static int getFaceId(FaceDirection face) {
        return switch (face) {
            case TOP  -> 0;
            case FRONT   -> 1;
            case BACK   -> 2;
            case RIGHT  -> 3;
            case LEFT    -> 4;
            case BOTTOM -> 5;
        };
    }

}
