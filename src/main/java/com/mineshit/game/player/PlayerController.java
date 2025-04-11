package com.mineshit.game.player;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.input.InputManager;
import com.mineshit.game.world.World;
import com.mineshit.game.world.generation.Chunk;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static org.lwjgl.glfw.GLFW.*;

public class PlayerController {

    private Vector3f position = new Vector3f(0, 50, 0);
    private Vector3f velocity = new Vector3f();

    private static final float MOUSE_SENSITIVITY = 0.1f;

    private static final float PLAYER_HEIGHT = 1.8f;
    private static final float PLAYER_RADIUS = 0.3f;

    private static final float MOVE_SPEED = 6.0f;
    private static final float JUMP_FORCE = 7.0f;
    private static final float GRAVITY = -20.0f;

    private boolean onGround = false;

    public void update(InputManager input, Camera camera, World world, float deltaTime) {
        handleInput(input, camera, deltaTime);
        applyGravity(deltaTime);
        move(world, deltaTime);
        updateCamera(camera);
        camera.rotate(input.getMouseDeltaX() * MOUSE_SENSITIVITY, input.getMouseDeltaY() * MOUSE_SENSITIVITY);

    }

    private void handleInput(InputManager input, Camera camera, float deltaTime) {
        Vector3f forward = new Vector3f(camera.getForward()).setComponent(1, 0).normalize();
        Vector3f right = new Vector3f(camera.getRight()).setComponent(1, 0).normalize();

        boolean isSprinting = false;

        Vector3f moveDir = new Vector3f();
        if (input.isKeyDown(GLFW_KEY_W)) moveDir.add(forward);
        if (input.isKeyDown(GLFW_KEY_S)) moveDir.sub(forward);
        if (input.isKeyDown(GLFW_KEY_D)) moveDir.add(right);
        if (input.isKeyDown(GLFW_KEY_A)) moveDir.sub(right);
        if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) isSprinting = true;

        if (moveDir.lengthSquared() > 0) {
            moveDir.normalize().mul(isSprinting ? MOVE_SPEED * 2 : MOVE_SPEED);
            velocity.x = moveDir.x;
            velocity.z = moveDir.z;
        } else {
            velocity.x = 0;
            velocity.z = 0;
        }

        if (onGround && input.isKeyDown(GLFW_KEY_SPACE)) {
            velocity.y = JUMP_FORCE;
            onGround = false;
        }
    }

    private void applyGravity(float deltaTime) {
        velocity.y += GRAVITY * deltaTime;
    }

    private void move(World world, float deltaTime) {
        Vector3f vertical = new Vector3f(position);
        vertical.fma(deltaTime, new Vector3f(0, velocity.y, 0));
        moveAxis(world, vertical, 1);

        Vector3f horizontal = new Vector3f(vertical);
        horizontal.fma(deltaTime, new Vector3f(velocity.x, 0, velocity.z));

        if (!collides(world, horizontal)) {
            position.set(horizontal);
            return;
        }

        Vector3f testX = new Vector3f(vertical);
        testX.fma(deltaTime, new Vector3f(velocity.x, 0, 0));
        if (!collides(world, testX)) {
            position.set(testX);
            return;
        }

        Vector3f testZ = new Vector3f(vertical);
        testZ.fma(deltaTime, new Vector3f(0, 0, velocity.z));
        if (!collides(world, testZ)) {
            position.set(testZ);
            return;
        }

        position.set(vertical);
    }


    private void moveAxis(World world, Vector3f nextPos, int axis) {
        float value = nextPos.get(axis);
        Vector3f testPos = new Vector3f(nextPos);
        testPos.setComponent(axis, value);

        if (collides(world, testPos)) {
            nextPos.setComponent(axis, position.get(axis));
            if (axis == 1 && velocity.y < 0) {
                onGround = true;
                velocity.y = 0;
            }
        }
    }


    private boolean collides(World world, Vector3f pos) {
        float eye = PLAYER_HEIGHT;
        boolean collided = false;

        for (float x = -PLAYER_RADIUS + 0.05f; x <= PLAYER_RADIUS - 0.05f; x += 0.4f)
            for (float y = 0.1f; y <= eye; y += 0.9f)
                for (float z = -PLAYER_RADIUS + 0.05f; z <= PLAYER_RADIUS - 0.05f; z += 0.4f) {
                    Vector3f sample = new Vector3f(pos).add(x, y, z);
                    Chunk chunk = world.getChunkAt(sample);
                    if (chunk != null) {
                        int bx = (int) Math.floor(sample.x);
                        int by = (int) Math.floor(sample.y);
                        int bz = (int) Math.floor(sample.z);
                        short block = chunk.getBlockAtWorld(bx, by, bz);

                        if (block != 0) {
                            collided = true;
                        }
                    }
                }

        return collided;
    }


    private void updateCamera(Camera camera) {
        Vector3f camPos = new Vector3f(position).add(0, PLAYER_HEIGHT - 0.1f, 0);
        camera.setPosition(camPos);
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
    }
}
