package com.mineshit.game.world.utils;

import com.mineshit.engine.game.ChunkMeshBuilder;
import com.mineshit.engine.game.ChunkMeshData;
import com.mineshit.engine.graphics.renderer.utils.Mesh;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.utils.FaceDirection;
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
    private Mesh shadowMesh;

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

                cleanupMesh();

                this.opaqueMesh = new Mesh(data.opaqueVertexBuffer(), data.opaqueIndexBuffer(), 7);
                this.transparentMesh = new Mesh(data.transparentVertexBuffer(), data.transparentIndexBuffer(), 7);
                this.shadowMesh = new Mesh(data.shadowVertexBuffer(), data.shadowIndexBuffer(), 7);
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
        ChunkMeshData data = ChunkMeshBuilder.buildBuffers(chunk, neighbors);

        if (data.canBeAdd()) {
            cleanupMesh();
            this.opaqueMesh = new Mesh(data.opaqueVertexBuffer(), data.opaqueIndexBuffer(), 7);
            this.transparentMesh = new Mesh(data.transparentVertexBuffer(), data.transparentIndexBuffer(), 7);
            this.shadowMesh = new Mesh(data.shadowVertexBuffer(), data.shadowIndexBuffer(), 7);
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
    }

    private void cleanupMeshDate(ChunkMeshData data){
        MemoryUtil.memFree(data.opaqueVertexBuffer());
        MemoryUtil.memFree(data.opaqueIndexBuffer());
        MemoryUtil.memFree(data.transparentVertexBuffer());
        MemoryUtil.memFree(data.transparentIndexBuffer());
        MemoryUtil.memFree(data.shadowVertexBuffer());
        MemoryUtil.memFree(data.shadowIndexBuffer());
    }

}
