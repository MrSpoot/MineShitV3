package com.mineshit.game.world.utils;

import com.mineshit.engine.game.ChunkMeshBuilder;
import com.mineshit.engine.game.ChunkMeshBuilderGreedy;
import com.mineshit.engine.game.ChunkMeshData;
import com.mineshit.engine.graphics.renderer.utils.Mesh;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.game.world.World;
import lombok.Getter;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ChunkRenderable {

    private static final ExecutorService meshingExecutor = Executors.newFixedThreadPool(
            1
    );

    private Future<ChunkMeshData> pendingMesh = null;

    @Getter
    private final Chunk chunk;
    private Mesh opaqueMesh;
    private Mesh transparentMesh;
    private Mesh shadowMesh;
    @Getter
    private FloatBuffer crossInstanceBuffer;
    @Getter
    private int crossInstanceCount;

    public ChunkRenderable(Chunk chunk) {
        this.chunk = chunk;
    }

    public void updateMeshIfNeeded(World world) {
        if ((chunk.getState() == ChunkState.DIRTY || chunk.getState() == ChunkState.GENERATED) && pendingMesh == null) {
            chunk.setState(ChunkState.MESHING);

            Map<FaceDirection, Chunk> neighbors = world.getNeighborChunks(chunk.getPosition());
            //pendingMesh = meshingExecutor.submit(() -> ChunkMeshBuilder.buildBuffers(chunk, neighbors));
            pendingMesh = meshingExecutor.submit(() -> ChunkMeshBuilderGreedy.buildBuffers(chunk, neighbors));
        }

        if (pendingMesh != null && pendingMesh.isDone()) {
            try {
                ChunkMeshData data = pendingMesh.get();
              
                cleanupMesh();

                this.opaqueMesh = new Mesh(data.opaqueVertexBuffer(), data.opaqueIndexBuffer(), 9);
                this.transparentMesh = new Mesh(data.transparentVertexBuffer(), data.transparentIndexBuffer(), 9);
                this.shadowMesh = new Mesh(data.shadowVertexBuffer(), data.shadowIndexBuffer(), 9);

                if (data.crossInstanceBuffer() != null) {
                    FloatBuffer src = data.crossInstanceBuffer();
                    src.rewind();
                    this.crossInstanceBuffer = MemoryUtil.memAllocFloat(src.remaining());
                    this.crossInstanceBuffer.put(src);
                    this.crossInstanceBuffer.flip();
                    this.crossInstanceCount = data.crossInstanceCount();
                }

                cleanupMeshDate(data);

                if(chunk.getState() != ChunkState.DIRTY) {
                    chunk.setState(ChunkState.MESHED);
                }

            } catch (Exception e) {
                chunk.setState(ChunkState.DIRTY);
            } finally {
                pendingMesh = null;
            }
        }
    }

    public void forceRebuild(World world) {
        if (pendingMesh != null && !pendingMesh.isDone()) {
            pendingMesh.cancel(true);
        }

        chunk.setState(ChunkState.MESHING);

        Map<FaceDirection, Chunk> neighbors = world.getNeighborChunks(chunk.getPosition());
        ChunkMeshData data = ChunkMeshBuilderGreedy.buildBuffers(chunk, neighbors);

        if (data.canBeAdd()) {
            cleanupMesh();
            this.opaqueMesh = new Mesh(data.opaqueVertexBuffer(), data.opaqueIndexBuffer(), 9);
            this.transparentMesh = new Mesh(data.transparentVertexBuffer(), data.transparentIndexBuffer(), 9);
            this.shadowMesh = new Mesh(data.shadowVertexBuffer(), data.shadowIndexBuffer(), 9);

            if (data.crossInstanceBuffer() != null) {
                FloatBuffer src = data.crossInstanceBuffer();
                src.rewind();
                this.crossInstanceBuffer = MemoryUtil.memAllocFloat(src.remaining());
                this.crossInstanceBuffer.put(src);
                this.crossInstanceBuffer.flip();
                this.crossInstanceCount = data.crossInstanceCount();
            }
        }
        cleanupMeshDate(data);
        chunk.setState(ChunkState.MESHED);
    }


    public void renderOpaque(World world, Shader shader) {
        if (opaqueMesh == null) return;

        Matrix4f model = new Matrix4f().translate(
                chunk.getPosition().x * Chunk.SIZE,
                chunk.getPosition().y * Chunk.SIZE,
                chunk.getPosition().z * Chunk.SIZE
        );
        shader.setUniform("uSunDir", world.getClock().getSunDirection());
        shader.setUniform("uModel", model);
        opaqueMesh.render();
    }

    public void renderTransparent(World world, Shader shader) {
        if (transparentMesh == null) return;

        Matrix4f model = new Matrix4f().translate(
                chunk.getPosition().x * Chunk.SIZE,
                chunk.getPosition().y * Chunk.SIZE,
                chunk.getPosition().z * Chunk.SIZE
        );
        shader.setUniform("uSunDir", world.getClock().getSunDirection());
        shader.setUniform("uModel", model);
        transparentMesh.render();
    }

    public void renderShadow(World world, Shader shadowShader, Matrix4f lightSpaceMatrix) {
        if (shadowMesh == null) return;

        Matrix4f model = new Matrix4f().translate(
                chunk.getPosition().x * Chunk.SIZE,
                chunk.getPosition().y * Chunk.SIZE,
                chunk.getPosition().z * Chunk.SIZE
        );

        shadowShader.setUniform("uModel", model);
        shadowShader.setUniform("uLightSpaceMatrix", lightSpaceMatrix);

        shadowMesh.render();
    }

    public static List<ChunkRenderable> getRenderableChunksFilterByFrustum(Collection<ChunkRenderable> chunks, Matrix4f frustumMatrix){
        FrustumIntersection intersection = new FrustumIntersection(frustumMatrix);

        return chunks.stream().filter((c) -> {
            Vector3f min = c.getChunk().getWorldMin();
            Vector3f max = c.getChunk().getWorldMax();
            return intersection.testAab(min.x, min.y, min.z, max.x, max.y, max.z);
        }).toList();
    }


    public boolean hasTransparent(){
        return transparentMesh != null;
    }

    public void cleanup() {
        cleanupMesh();
        if(pendingMesh != null) pendingMesh.cancel(true);
    }

    public static void cleanupStatic(){
        meshingExecutor.shutdownNow();
    }

    private void cleanupMesh(){
        if(opaqueMesh != null) opaqueMesh.cleanup();
        if(transparentMesh != null) transparentMesh.cleanup();
        if(shadowMesh != null) shadowMesh.cleanup();
        if(crossInstanceBuffer != null) {
            MemoryUtil.memFree(crossInstanceBuffer);
            crossInstanceBuffer = null;
            crossInstanceCount = 0;
        }
    }

    private void cleanupMeshDate(ChunkMeshData data){
        MemoryUtil.memFree(data.opaqueVertexBuffer());
        MemoryUtil.memFree(data.opaqueIndexBuffer());
        MemoryUtil.memFree(data.transparentVertexBuffer());
        MemoryUtil.memFree(data.transparentIndexBuffer());
        MemoryUtil.memFree(data.shadowVertexBuffer());
        MemoryUtil.memFree(data.shadowIndexBuffer());
        MemoryUtil.memFree(data.crossInstanceBuffer());
    }

}
