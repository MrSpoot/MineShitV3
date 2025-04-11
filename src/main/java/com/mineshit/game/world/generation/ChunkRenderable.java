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
    private Mesh mesh;

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

                if(data.canBeAdd()){
                    this.mesh = new Mesh(data.vertexBuffer(), data.indexBuffer(), 7);
                }

                MemoryUtil.memFree(data.vertexBuffer());
                MemoryUtil.memFree(data.indexBuffer());

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

    public void render(Shader shader) {
        if (mesh == null) return;

        Matrix4f model = new Matrix4f().translate(
                chunk.getPosition().x * Chunk.SIZE,
                chunk.getPosition().y * Chunk.SIZE,
                chunk.getPosition().z * Chunk.SIZE
        );
        shader.setUniform("uModel", model);
        mesh.render();

        Statistic.increment("Drawcalls");
    }

    public void cleanup() {
        if (mesh != null) mesh.cleanup();
        if(pendingMesh != null) pendingMesh.cancel(true);
    }

    public static void cleanupStatic(){
        meshingExecutor.shutdownNow();
    }

}
