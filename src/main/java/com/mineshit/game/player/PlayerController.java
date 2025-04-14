package com.mineshit.game.player;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.input.InputManager;
import com.mineshit.game.world.World;
import com.mineshit.game.world.utils.Chunk;
import com.mineshit.game.world.utils.BlockType;
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

        float density = getDensityAt(world, horizontal);
        if (density >= 1.0f) {
            // Bloc solide, on vérifie les axes séparément
            Vector3f testX = new Vector3f(vertical).fma(deltaTime, new Vector3f(velocity.x, 0, 0));
            if (getDensityAt(world, testX) < 1.0f) {
                position.set(testX);
                return;
            }

            Vector3f testZ = new Vector3f(vertical).fma(deltaTime, new Vector3f(0, 0, velocity.z));
            if (getDensityAt(world, testZ) < 1.0f) {
                position.set(testZ);
                return;
            }

            position.set(vertical);
        } else {
            // Pas totalement bloqué : on ralentit
            float slowFactor = 1.0f - density; // ex: densité = 0.3 → 70% de vitesse
            horizontal.sub(vertical).mul(slowFactor).add(vertical); // interpolation ralentie
            position.set(horizontal);
        }
    }



    private void moveAxis(World world, Vector3f nextPos, int axis) {
        float value = nextPos.get(axis);
        Vector3f testPos = new Vector3f(nextPos);
        testPos.setComponent(axis, value);

        float density = getDensityAt(world, testPos);
        if (density >= 1.0f) {
            // Collision solide
            nextPos.setComponent(axis, position.get(axis));

            if (axis == 1 && velocity.y < 0) {
                onGround = true;
                velocity.y = 0;
            }
        } else if (density > 0.0f) {
            // Traversable mais ralentit (optionnel ici)
            float slow = 1.0f - density;
            float original = nextPos.get(axis);
            float interpolated = position.get(axis) + (original - position.get(axis)) * slow;
            nextPos.setComponent(axis, interpolated);
        }
    }



    private float getDensityAt(World world, Vector3f pos) {
        float eye = PLAYER_HEIGHT;
        float maxDensity = 0.0f;

        for (float x = -PLAYER_RADIUS + 0.05f; x <= PLAYER_RADIUS - 0.05f; x += 0.4f)
            for (float y = 0.1f; y <= eye; y += 0.4f)
                for (float z = -PLAYER_RADIUS + 0.05f; z <= PLAYER_RADIUS - 0.05f; z += 0.4f) {
                    Vector3f sample = new Vector3f(pos).add(x, y, z);
                    Chunk chunk = world.getChunkAt(sample);
                    if (chunk != null) {
                        int bx = (int) Math.floor(sample.x);
                        int by = (int) Math.floor(sample.y);
                        int bz = (int) Math.floor(sample.z);
                        short id = chunk.getBlockAtWorld(bx, by, bz);

                        if (id != 0) {
                            float density = BlockType.fromId(id).getDensity();
                            maxDensity = Math.max(maxDensity, density);
                        }
                    }
                }

        return maxDensity;
    }


    public boolean isOccupying(Vector3i blockPos) {
        float minX = position.x - PLAYER_RADIUS;
        float maxX = position.x + PLAYER_RADIUS;
        float minY = position.y;
        float maxY = position.y + PLAYER_HEIGHT;
        float minZ = position.z - PLAYER_RADIUS;
        float maxZ = position.z + PLAYER_RADIUS;

        int bx = blockPos.x;
        int by = blockPos.y;
        int bz = blockPos.z;

        return bx + 1 > minX && bx < maxX &&
                by + 1 > minY && by < maxY &&
                bz + 1 > minZ && bz < maxZ;
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
