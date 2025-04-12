package com.mineshit.game.world.generation;

import com.mineshit.engine.game.ChunkMeshBuilder;
import com.mineshit.engine.game.ChunkMeshData;
import com.mineshit.engine.graphics.renderer.Mesh;
import com.mineshit.engine.graphics.renderer.Shader;
import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.engine.utils.Statistic;
import com.mineshit.game.world.World;
import lombok.Getter;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

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

    public ChunkRenderable(Chunk chunk) {
        this.chunk = chunk;
    }

    public void updateMeshIfNeeded(World world) {
        if ((chunk.getState() == ChunkState.DIRTY || chunk.getState() == ChunkState.GENERATED) && pendingMesh == null) {
            chunk.setState(ChunkState.MESHING);

            Map<FaceDirection, Chunk> neighbors = world.getNeighborChunks(chunk.getPosition());
            pendingMesh = meshingExecutor.submit(() -> ChunkMeshBuilder.buildBuffers(chunk, neighbors));
        }

        if (pendingMesh != null && pendingMesh.isDone()) {
            try {
                ChunkMeshData data = pendingMesh.get();

                this.opaqueMesh = new Mesh(data.opaqueVertexBuffer(), data.opaqueIndexBuffer(), 7);
                this.transparentMesh = new Mesh(data.transparentVertexBuffer(), data.transparentIndexBuffer(), 7);


                MemoryUtil.memFree(data.opaqueVertexBuffer());
                MemoryUtil.memFree(data.opaqueIndexBuffer());

                MemoryUtil.memFree(data.transparentVertexBuffer());
                MemoryUtil.memFree(data.transparentIndexBuffer());

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
        ChunkMeshData data = ChunkMeshBuilder.buildBuffers(chunk, neighbors);

        if (data.canBeAdd()) {
            if (opaqueMesh != null) opaqueMesh.cleanup();
            this.opaqueMesh = new Mesh(data.opaqueVertexBuffer(), data.opaqueIndexBuffer(), 7);
            if (transparentMesh != null) transparentMesh.cleanup();
            this.transparentMesh = new Mesh(data.transparentVertexBuffer(), data.transparentIndexBuffer(), 7);
        }

        MemoryUtil.memFree(data.opaqueVertexBuffer());
        MemoryUtil.memFree(data.opaqueIndexBuffer());
        MemoryUtil.memFree(data.transparentVertexBuffer());
        MemoryUtil.memFree(data.transparentIndexBuffer());

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

    public boolean hasTransparent(){
        return transparentMesh != null;
    }

    public void cleanup() {
        if (opaqueMesh != null) opaqueMesh.cleanup();
        if (transparentMesh != null) transparentMesh.cleanup();
        if(pendingMesh != null) pendingMesh.cancel(true);
    }

    public static void cleanupStatic(){
        meshingExecutor.shutdownNow();
    }

}
