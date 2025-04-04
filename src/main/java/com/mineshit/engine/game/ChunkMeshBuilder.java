package com.mineshit.engine.game;

import com.mineshit.engine.graphics.Mesh;
import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.game.world.Chunk;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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

    public static Mesh build(Chunk chunk) {
        long startTime = System.nanoTime();
        LOGGER.trace("Building Mesh");

        int maxFaces = Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 6;
        FloatBuffer vertexBuffer = memAllocFloat(maxFaces * 4 * 6); // 4 vertices per face, 6 floats per vertex
        IntBuffer indexBuffer = memAllocInt(maxFaces * 6); // 6 indices per face

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

                        int faceId = getFaceId(face);

                        if (chunk.isOutOfBounds(nx, ny, nz) || chunk.getBlock(nx, ny, nz) == 0) {
                            float[] faceVertices = getFaceVertices(x, y, z, face);
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

        Mesh mesh = new Mesh(vertexBuffer, indexBuffer, 7); // 6 floats per vertex

        MemoryUtil.memFree(vertexBuffer);
        MemoryUtil.memFree(indexBuffer);

        return mesh;
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
            case FRONT  -> 0;
            case BACK   -> 1;
            case LEFT   -> 2;
            case RIGHT  -> 3;
            case TOP    -> 4;
            case BOTTOM -> 5;
        };
    }

}
