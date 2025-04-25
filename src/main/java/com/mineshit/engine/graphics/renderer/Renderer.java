package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.graphics.renderer.passes.*;
import com.mineshit.engine.graphics.renderer.utils.GBuffer;
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

    private final Pipeline pipeline = new Pipeline();

    public void init(Window window) {
        LOGGER.info("Initializing Renderer");

        this.pipeline.addPass(new SkyBoxPass());
        this.pipeline.addPass(new ChunkShadowPass());
        this.pipeline.addPass(new ChunkOpaquePass());
        this.pipeline.addPass(new ChunkTransparentPass());
        this.pipeline.addPass(new InterfacePass());

        this.pipeline.init(window);
    }

    public void render(Window window, PlayerController playerController, InputManager input, Camera camera, World world, float alpha) {
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        pipeline.render(window,input,world,camera, playerController);

        Statistic.set("Drawcalls",0L);
    }

    public void cleanup() {
        pipeline.cleanup();
    }
}

