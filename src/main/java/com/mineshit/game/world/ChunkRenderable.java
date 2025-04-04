package com.mineshit.game.world;

import com.mineshit.engine.game.ChunkMeshBuilder;
import com.mineshit.engine.graphics.Mesh;
import com.mineshit.engine.graphics.Shader;
import lombok.Getter;
import org.joml.Matrix4f;

public class ChunkRenderable {
    @Getter
    private final Chunk chunk;
    private Mesh mesh;

    public ChunkRenderable(Chunk chunk) {
        this.chunk = chunk;
    }

    public void updateMeshIfNeeded(World world) {
        if (chunk.getState() == ChunkState.GENERATED || chunk.getState() == ChunkState.DIRTY) {
            if (mesh != null) mesh.cleanup();
            mesh = ChunkMeshBuilder.build(chunk, world.getNeighborChunks(chunk.getPosition()));
            chunk.setState(ChunkState.MESHED);
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
    }

    public void cleanup() {
        if (mesh != null) mesh.cleanup();
    }

}
