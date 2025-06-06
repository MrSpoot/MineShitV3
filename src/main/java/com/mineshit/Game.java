package com.mineshit;

import com.mineshit.engine.core.Timer;
import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.graphics.renderer.Pipeline;
import com.mineshit.engine.graphics.renderer.Renderer;
import com.mineshit.engine.graphics.renderer.passes.DebugPass;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.engine.input.InputManager;
import com.mineshit.engine.utils.Statistic;
import com.mineshit.engine.window.Window;
import com.mineshit.game.player.PlayerController;
import com.mineshit.game.world.World;
import com.mineshit.game.world.utils.Chunk;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Game {

    private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);

    private Window window;
    private Camera camera;

    private Timer logicTimer;
    private Timer renderTimer;

    private Renderer renderer;
    private InputManager input;

    private PlayerController playerController;

    private World world;

    private int frameCount = 0;
    private double lastFpsTime = 0;

    private boolean canToggleDebug = true;
    private boolean canSwitchRenderMode = true;


    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        LOGGER.info("Initializing");

        window = new Window("MineShit", 1280, 720 );
        window.create();

        camera = new Camera(70f, window.aspectRatio());
        camera.move(new Vector3f(0, 0, 3));

        logicTimer = new Timer(60);
        renderTimer = new Timer(240);

        renderer = new Renderer();
        renderer.init(window);

        playerController = new PlayerController();

        input = new InputManager(window.getId());

        TextureManager.init();

        world = new World();

        //world.getClock().pause();

    }

    private void loop() {
        LOGGER.info("Starting game loop");

        while (!window.shouldClose()) {
            logicTimer.update();
            renderTimer.update();

            while (logicTimer.shouldTick()) {
                update(logicTimer.getFixedStep());
            }

            if (renderTimer.shouldTick()) {
                float alpha = renderTimer.getAlpha();
                render(alpha);
                window.update();
                frameCount++;

                double currentTime = glfwGetTime();
                if (currentTime - lastFpsTime >= 1.0) {
                    Statistic.set("FPS", frameCount);
                    frameCount = 0;
                    lastFpsTime = currentTime;
                }
            }
        }
    }


    private void update(float deltaTime) {
        input.update();

        playerController.update(input,camera,world,deltaTime);

        if (input.isKeyDown(GLFW_KEY_ESCAPE)) window.close();

        if (input.isKeyDown(GLFW_KEY_P)) glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        if (input.isKeyDown(GLFW_KEY_O)) glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        if(input.isKeyDown(GLFW_KEY_Y)) world.getClock().pause();
        if(input.isKeyDown(GLFW_KEY_U)) world.getClock().resume();

        if (input.isKeyDown(GLFW_KEY_O)) {
            if (canToggleDebug) {
                Pipeline.renderDebug = !Pipeline.renderDebug;
                canToggleDebug = false;
            }
        } else {
            canToggleDebug = true;
        }

        if (input.isKeyDown(GLFW_KEY_L)) {
            if (canSwitchRenderMode) {
                DebugPass.nextRenderMode();
                canSwitchRenderMode = false;
            }
        } else {
            canSwitchRenderMode = true;
        }


        Statistic.set("Camera Position","X : "+String.format("%.1f",camera.getPosition().x)+" | Y : "+String.format("%.1f",camera.getPosition().y)+" | Z : "+String.format("%.1f",camera.getPosition().z));
        Statistic.set("Chunk Position","X : "+getChunkPosition(camera.getPosition()).x+" | Y : "+getChunkPosition(camera.getPosition()).y+" | Z : "+getChunkPosition(camera.getPosition()).z);

        world.update(camera.getPosition());
    }

    public static Vector3i getChunkPosition(Vector3f worldPos) {
        int chunkX = (int)Math.floor(worldPos.x / Chunk.SIZE);
        int chunkY = (int)Math.floor(worldPos.y / Chunk.SIZE);
        int chunkZ = (int)Math.floor(worldPos.z / Chunk.SIZE);
        return new Vector3i(chunkX, chunkY, chunkZ);
    }



    private void render(float alpha) {
        renderer.render(window,playerController, input, camera,world,alpha);
    }

    private void cleanup() {
        LOGGER.info("Cleaning up");
        world.cleanup();
        LOGGER.info("Cleanup renderer");
        renderer.cleanup();
        LOGGER.info("Cleanup windows");
        window.cleanup();
    }

    public static void main(String[] args) {
        new Game().run();
    }
}
