package com.mineshit;

import com.mineshit.engine.core.Timer;
import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.graphics.Renderer;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.engine.input.InputManager;
import com.mineshit.engine.window.Window;
import org.joml.Vector3f;
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

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        LOGGER.info("Initializing");

        window = new Window("MineShit", 1280, 720);
        window.create();

        camera = new Camera(70f, window.aspectRatio());
        camera.move(new Vector3f(0, 0, 3));

        logicTimer = new Timer(60);
        renderTimer = new Timer(240);

        renderer = new Renderer();
        renderer.init();

        input = new InputManager(window.getId());

        TextureManager.init();

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
            }
        }

    }

    private void update(float deltaTime) {
        input.update();

        if (input.isKeyDown(GLFW_KEY_ESCAPE)) window.close();

        float speed = 5f * deltaTime;
        float sensitivity = 0.1f;

        Vector3f move = new Vector3f();
        if(input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) speed *= 2;
        if (input.isKeyDown(GLFW_KEY_W)) move.z += speed;
        if (input.isKeyDown(GLFW_KEY_S)) move.z -= speed;
        if (input.isKeyDown(GLFW_KEY_A)) move.x -= speed;
        if (input.isKeyDown(GLFW_KEY_D)) move.x += speed;

        if (input.isKeyDown(GLFW_KEY_P)) glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        if (input.isKeyDown(GLFW_KEY_O)) glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        camera.moveRelative(move);

        camera.rotate(input.getMouseDeltaX() * sensitivity, input.getMouseDeltaY() * sensitivity);
    }


    private void render(float alpha) {
        renderer.render(camera,alpha);
    }

    private void cleanup() {
        LOGGER.info("Cleaning up");
        renderer.cleanup();
        window.cleanup();
    }

    public static void main(String[] args) {
        new Game().run();
    }
}
