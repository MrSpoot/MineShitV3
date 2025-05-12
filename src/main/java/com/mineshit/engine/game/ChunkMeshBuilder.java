package com.mineshit.engine.game;

import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.game.world.utils.BlockType;
import com.mineshit.game.world.utils.Chunk;
import com.mineshit.game.world.utils.TransparencyType;
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
        LOGGER.trace("Building Mesh");

        int maxFaces = Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 6;
        FloatBuffer opaqueVertexBuffer = memAllocFloat(maxFaces * 4 * 7); // 7 floats per vertex now (added faceId)
        IntBuffer opaqueIndexBuffer = memAllocInt(maxFaces * 6);

        FloatBuffer transparentVertexBuffer = memAllocFloat(maxFaces * 4 * 7); // 7 floats per vertex now (added faceId)
        IntBuffer transparentIndexBuffer = memAllocInt(maxFaces * 6);

        FloatBuffer shadowVertexBuffer = memAllocFloat(maxFaces * 4 * 7); // 7 floats per vertex now (added faceId)
        IntBuffer shadowIndexBuffer = memAllocInt(maxFaces * 6);

        int opaqueIndexOffset = 0;
        int opaqueVertexCount = 0;

        int transparentIndexOffset = 0;
        int transparentVertexCount = 0;

        int shadowIndexOffset = 0;
        int shadowVertexCount = 0;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    short block = chunk.getBlock(x, y, z);
                    if (block == 0) continue;

                    BlockType blockType = BlockType.fromId(block);

                    if(blockType.getTransparencyType().equals(TransparencyType.TRANSLUCENT) || blockType.getTransparencyType().equals(TransparencyType.TRANSPARENT)) {
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

                            BlockType neighborBlockType = BlockType.fromId(neighborBlock);

                            if (shouldCullFace(blockType,neighborBlockType)) continue;

                            float[] faceVertices = getFaceVertices(x, y, z, face);
                            int faceId = getFaceId(face);

                            for (int i = 0; i < 4; i++) {
                                transparentVertexBuffer.put(faceVertices[i * 3]);
                                transparentVertexBuffer.put(faceVertices[i * 3 + 1]);
                                transparentVertexBuffer.put(faceVertices[i * 3 + 2]);
                                transparentVertexBuffer.put(UVS[i][0]);
                                transparentVertexBuffer.put(UVS[i][1]);
                                transparentVertexBuffer.put(block);
                                transparentVertexBuffer.put(faceId);
                            }

                            transparentIndexBuffer.put(transparentIndexOffset);
                            transparentIndexBuffer.put(transparentIndexOffset + 1);
                            transparentIndexBuffer.put(transparentIndexOffset + 2);
                            transparentIndexBuffer.put(transparentIndexOffset + 2);
                            transparentIndexBuffer.put(transparentIndexOffset + 3);
                            transparentIndexBuffer.put(transparentIndexOffset);
                            transparentIndexOffset += 4;
                            transparentVertexCount += 4;

                            //appendFaceToBuffer(shadowVertexBuffer, shadowIndexBuffer, faceVertices, faceId, block, shadowIndexOffset);
                            shadowIndexOffset += 4;
                            shadowVertexCount += 4;
                        }

                    }else{
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

                            BlockType neighborBlockType = BlockType.fromId(neighborBlock);

                            if (shouldCullFace(blockType,neighborBlockType)) continue;

                            float[] faceVertices = getFaceVertices(x, y, z, face);
                            int faceId = getFaceId(face);

                            for (int i = 0; i < 4; i++) {
                                opaqueVertexBuffer.put(faceVertices[i * 3]);
                                opaqueVertexBuffer.put(faceVertices[i * 3 + 1]);
                                opaqueVertexBuffer.put(faceVertices[i * 3 + 2]);
                                opaqueVertexBuffer.put(UVS[i][0]);
                                opaqueVertexBuffer.put(UVS[i][1]);
                                opaqueVertexBuffer.put(block);
                                opaqueVertexBuffer.put(faceId);
                            }

                            opaqueIndexBuffer.put(opaqueIndexOffset);
                            opaqueIndexBuffer.put(opaqueIndexOffset + 1);
                            opaqueIndexBuffer.put(opaqueIndexOffset + 2);
                            opaqueIndexBuffer.put(opaqueIndexOffset + 2);
                            opaqueIndexBuffer.put(opaqueIndexOffset + 3);
                            opaqueIndexBuffer.put(opaqueIndexOffset);
                            opaqueIndexOffset += 4;
                            opaqueVertexCount += 4;

                            appendFaceToBuffer(shadowVertexBuffer, shadowIndexBuffer, faceVertices, faceId, block, shadowIndexOffset);
                            shadowIndexOffset += 4;
                            shadowVertexCount += 4;
                        }
                    }
                }
            }
        }

        opaqueVertexBuffer.flip();
        opaqueIndexBuffer.flip();

        transparentVertexBuffer.flip();
        transparentIndexBuffer.flip();

        shadowVertexBuffer.flip();
        shadowIndexBuffer.flip();

        return new ChunkMeshData(opaqueVertexBuffer, opaqueIndexBuffer, opaqueVertexCount, transparentVertexBuffer, transparentIndexBuffer, transparentVertexCount, shadowVertexBuffer, shadowIndexBuffer, shadowVertexCount);
    }

    private static void appendFaceToBuffer(
            FloatBuffer vertexBuffer,
            IntBuffer indexBuffer,
            float[] faceVertices,
            int faceId,
            short blockId,
            int vertexOffset
    ) {
        for (int i = 0; i < 4; i++) {
            vertexBuffer.put(faceVertices[i * 3]);      // pos.x
            vertexBuffer.put(faceVertices[i * 3 + 1]);  // pos.y
            vertexBuffer.put(faceVertices[i * 3 + 2]);  // pos.z
            vertexBuffer.put(UVS[i][0]);                // uv.x
            vertexBuffer.put(UVS[i][1]);                // uv.y
            vertexBuffer.put(blockId);                  // texture index
            vertexBuffer.put(faceId);                   // face index
        }

        indexBuffer.put(vertexOffset);
        indexBuffer.put(vertexOffset + 1);
        indexBuffer.put(vertexOffset + 2);
        indexBuffer.put(vertexOffset + 2);
        indexBuffer.put(vertexOffset + 3);
        indexBuffer.put(vertexOffset);
    }


    private static boolean shouldCullFace(BlockType current, BlockType neighbor) {
        return switch (current.getCullingMode()) {
            case NONE -> false;

            case ALWAYS_CULL -> neighbor != BlockType.AIR;

            case CULL_IF_OPAQUE -> neighbor != BlockType.AIR
                    && neighbor.getTransparencyType() == TransparencyType.OPAQUE;

            case CULL_IF_SAME -> neighbor == current;

            case CULL_IF_SOLID -> neighbor != BlockType.AIR
                    && (neighbor.getTransparencyType() == TransparencyType.OPAQUE
                    || neighbor.getTransparencyType() == TransparencyType.CUTOUT);
            case CULL_IF_SAME_OR_OPAQUE -> (neighbor == current) || (neighbor != BlockType.AIR
                    && neighbor.getTransparencyType() == TransparencyType.OPAQUE);
        };
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
