package com.mineshit.engine.game;

import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.game.world.utils.BlockType;
import com.mineshit.game.world.utils.Chunk;
import com.mineshit.game.world.utils.TransparencyType;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;

import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memAllocInt;

public class ChunkMeshBuilderGreedy {

    private static final float[][] UVS = {
            {0f, 0f}, {1f, 0f}, {1f, 1f}, {0f, 1f}
    };

    public static ChunkMeshData buildBuffers(Chunk chunk, Map<FaceDirection, Chunk> neighbors) {
        FloatBuffer vertexBuffer = memAllocFloat(Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 4 * 7);
        IntBuffer indexBuffer = memAllocInt(Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 6);
        int vertexOffset = 0;

        for (FaceDirection face : FaceDirection.values()) {
            vertexOffset = greedyMeshFace(chunk, neighbors, face, vertexBuffer, indexBuffer, vertexOffset);
        }

        vertexBuffer.flip();
        indexBuffer.flip();

        return new ChunkMeshData(
                vertexBuffer, indexBuffer, vertexOffset,
                null, null, 0, null, null, 0,
                null, 0
        );
    }

    private static int greedyMeshFace(Chunk chunk, Map<FaceDirection, Chunk> neighbors,
                                      FaceDirection face, FloatBuffer vbo, IntBuffer ibo, int vertexOffsetStart) {
        int size = Chunk.SIZE;
        short[][] mask = new short[size][size];

        int axisU = face.axisU();
        int axisV = face.axisV();
        int axisW = face.axisW();

        for (int w = 0; w < size; w++) {
            for (int u = 0; u < size; u++) {
                for (int v = 0; v < size; v++) {
                    int[] pos = {0, 0, 0};
                    pos[axisU] = u;
                    pos[axisV] = v;
                    pos[axisW] = w;
                    int x = pos[0], y = pos[1], z = pos[2];

                    BlockType block = BlockType.fromId(chunk.getBlock(x, y, z));
                    if (block == BlockType.AIR || block.getTransparencyType() != TransparencyType.OPAQUE) {
                        mask[u][v] = -1;
                        continue;
                    }

                    int nx = x + face.getOffsetX();
                    int ny = y + face.getOffsetY();
                    int nz = z + face.getOffsetZ();

                    short neighborBlock = chunk.isInBounds(nx, ny, nz)
                            ? chunk.getBlock(nx, ny, nz)
                            : (neighbors.get(face) != null
                            ? neighbors.get(face).getBlock((nx + size) % size, (ny + size) % size, (nz + size) % size)
                            : 0);

                    BlockType neighbor = BlockType.fromId(neighborBlock);
                    if (neighbor != BlockType.AIR && neighbor.getTransparencyType() == TransparencyType.OPAQUE) {
                        mask[u][v] = -1;
                    } else {
                        mask[u][v] = block.getId();
                    }
                }
            }

            for (int u = 0; u < size; u++) {
                for (int v = 0; v < size; v++) {
                    short id = mask[u][v];
                    if (id == -1) continue;

                    int width = 1;
                    while (u + width < size && mask[u + width][v] == id) width++;

                    int height = 1;
                    while (v + height < size) {
                        boolean same = true;
                        for (int i = 0; i < width; i++) {
                            if (mask[u + i][v + height] != id) {
                                same = false;
                                break;
                            }
                        }
                        if (!same) break;
                        height++;
                    }

                    for (int dx = 0; dx < width; dx++)
                        for (int dy = 0; dy < height; dy++)
                            mask[u + dx][v + dy] = -1;

                    vertexOffsetStart = emitQuad(u, v, width, height, w, face, id, vbo, ibo, vertexOffsetStart);
                }
            }
        }

        return vertexOffsetStart;
    }

    private static int emitQuad(int u, int v, int w, int h, int depth,
                                FaceDirection face, short blockId,
                                FloatBuffer vbo, IntBuffer ibo, int vertexOffset) {

        float wF = w;
        float hF = h;
        float d = depth;

        float[][] corners = switch (face) {
            case FRONT -> new float[][] {
                    {u,     v,     d + 1},
                    {u + wF, v,     d + 1},
                    {u + wF, v + hF, d + 1},
                    {u,     v + hF, d + 1}
            };
            case BACK -> new float[][] {
                    {u + wF, v,     d},
                    {u,     v,     d},
                    {u,     v + hF, d},
                    {u + wF, v + hF, d}
            };
            case LEFT -> new float[][] {
                    {d + 1,     v,     u + wF},
                    {d + 1,     v,     u},
                    {d + 1,     v + hF, u},
                    {d + 1 ,     v + hF, u + wF}
            };
            case RIGHT -> new float[][] {
                    {d, v,     u},
                    {d, v,     u + wF},
                    {d, v + hF, u + wF},
                    {d, v + hF, u}
            };
            case BOTTOM -> new float[][] {
                    {u,     d,     v},
                    {u + wF, d,     v},
                    {u + wF, d,     v + hF},
                    {u,     d,     v + hF}
            };
            case TOP -> new float[][] {
                    {u,     d + 1, v + hF},
                    {u + wF, d + 1, v + hF},
                    {u + wF, d + 1, v},
                    {u,     d + 1, v}
            };
        };

        for (int i = 0; i < 4; i++) {
            vbo.put(corners[i][0]);
            vbo.put(corners[i][1]);
            vbo.put(corners[i][2]);
            vbo.put(UVS[i][0]);
            vbo.put(UVS[i][1]);
            vbo.put(blockId);
            vbo.put(getFaceId(face));
        }

        ibo.put(vertexOffset);
        ibo.put(vertexOffset + 1);
        ibo.put(vertexOffset + 2);
        ibo.put(vertexOffset + 2);
        ibo.put(vertexOffset + 3);
        ibo.put(vertexOffset);

        return vertexOffset + 4;
    }

    private static int getFaceId(FaceDirection face) {
        return switch (face) {
            case TOP -> 0;
            case FRONT -> 1;
            case BACK -> 2;
            case RIGHT -> 3;
            case LEFT -> 4;
            case BOTTOM -> 5;
        };
    }
}

