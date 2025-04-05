package com.mineshit.engine.graphics;

import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.engine.window.Window;
import com.mineshit.game.world.Chunk;
import com.mineshit.game.world.ChunkRenderable;
import com.mineshit.game.world.ChunkState;
import com.mineshit.game.world.World;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {

    private final SkyBoxRenderer skyBoxRenderer = new SkyBoxRenderer();
    private final ChunkRenderer chunkRenderer = new ChunkRenderer();

    public void init() {
        skyBoxRenderer.init();
        chunkRenderer.init();
    }

    public void render(Window window, Camera camera, World world, float alpha) {
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        skyBoxRenderer.render(camera,world,alpha);
        chunkRenderer.render(camera,world,alpha);

    }

    public void cleanup() {
        skyBoxRenderer.cleanup();
        chunkRenderer.cleanup();
    }
}

