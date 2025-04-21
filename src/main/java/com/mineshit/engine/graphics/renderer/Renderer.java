package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.input.InputManager;
import com.mineshit.engine.utils.Statistic;
import com.mineshit.engine.window.Window;
import com.mineshit.game.player.PlayerController;
import com.mineshit.game.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Renderer.class);

    private final SkyBoxRenderer skyBoxRenderer = new SkyBoxRenderer();
    private final ChunkRenderer chunkRenderer = new ChunkRenderer();
    private final SelectionRenderer selectionRenderer = new SelectionRenderer();
    private final InterfaceRenderer interfaceRenderer = new InterfaceRenderer();

    public void init() {
        LOGGER.info("Initializing Renderer");
        skyBoxRenderer.init();
        chunkRenderer.init();
        selectionRenderer.init();
        interfaceRenderer.init();
    }

    public void render(Window window, PlayerController playerController, InputManager input, Camera camera, World world, float alpha) {
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        skyBoxRenderer.render(camera,world,alpha);
        chunkRenderer.render(window,input, playerController, camera,world,alpha);
        selectionRenderer.render(camera,world,alpha);
        interfaceRenderer.render(window);

        Statistic.set("Drawcalls",0L);
    }

    public void cleanup() {
        skyBoxRenderer.cleanup();
        chunkRenderer.cleanup();
        selectionRenderer.cleanup();
        interfaceRenderer.cleanup();
    }
}

