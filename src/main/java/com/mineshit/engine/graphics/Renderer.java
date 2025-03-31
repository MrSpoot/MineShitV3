package com.mineshit.engine.graphics;

import com.mineshit.engine.game.ChunkMeshBuilder;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.game.world.Chunk;
import org.joml.Matrix4f;
import org.joml.Vector3i;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {

    private Shader shader;
    private Mesh mesh;
    private Chunk chunk;

    public void init() {
        shader = new Shader("/shaders/basic.glsl");

        chunk = new Chunk(new Vector3i(0,-1,0));

        chunk.fillChunk((short) 1);

        //chunk.setBlock(0,0,0,(short)0);
        chunk.setBlock(0,1,0,(short)2);


    }

    public void render(Camera camera, float alpha) {
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.useProgram();

        TextureManager.BLOCK_TEXTURES.bind(0);

        shader.setUniform("uProjection", camera.getProjectionMatrix());
        shader.setUniform("uView", camera.getViewMatrix());
        shader.setUniform("uModel", new Matrix4f().translate(
                chunk.getPosition().x * Chunk.SIZE,
                chunk.getPosition().y * Chunk.SIZE,
                chunk.getPosition().z * Chunk.SIZE
        ));

        mesh = ChunkMeshBuilder.build(chunk);

        mesh.render();

        TextureManager.BLOCK_TEXTURES.unbind();

        shader.unbind();
    }

    public void cleanup() {
        shader.destroy();
        mesh.cleanup();
    }
}
