package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.input.InputManager;
import com.mineshit.engine.window.Window;
import com.mineshit.game.world.World;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {

    private final SkyBoxRenderer skyBoxRenderer = new SkyBoxRenderer();
    private final ChunkRenderer chunkRenderer = new ChunkRenderer();
    private final SelectionRenderer selectionRenderer = new SelectionRenderer();

    public void init() {
        skyBoxRenderer.init();
        chunkRenderer.init();
        selectionRenderer.init();
    }

    public void render(Window window, InputManager input, Camera camera, World world, float alpha) {
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        skyBoxRenderer.render(camera,world,alpha);
        chunkRenderer.render(input, camera,world,alpha);
        selectionRenderer.render(camera,world,alpha);

    }

    public void cleanup() {
        skyBoxRenderer.cleanup();
        chunkRenderer.cleanup();
    }
}

