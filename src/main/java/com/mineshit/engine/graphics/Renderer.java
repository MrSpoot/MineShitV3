package com.mineshit.engine.graphics;

import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.game.world.ChunkRenderable;
import com.mineshit.game.world.World;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {

    private Shader shader;

    public void init() {
        shader = new Shader("/shaders/basic.glsl");
    }

    public void render(Camera camera, World world, float alpha) {
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.useProgram();
        TextureManager.BLOCK_TEXTURES.bind(0);

        shader.setUniform("uProjection", camera.getProjectionMatrix());
        shader.setUniform("uView", camera.getViewMatrix());

        for (ChunkRenderable renderable : world.getRenderables()) {
            renderable.updateMeshIfNeeded();
            renderable.render(shader);
        }

        TextureManager.BLOCK_TEXTURES.unbind();
        shader.unbind();
    }

    public void cleanup(World world) {
        shader.destroy();
        for (ChunkRenderable renderable : world.getRenderables()) {
            renderable.cleanup();
        }
    }
}

