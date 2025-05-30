package com.mineshit.game.world.interaction;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.input.InputManager;
import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.engine.utils.Statistic;
import com.mineshit.game.player.PlayerController;
import com.mineshit.game.world.World;
import com.mineshit.game.world.utils.BlockType;
import com.mineshit.game.world.utils.Chunk;
import com.mineshit.game.world.utils.ChunkState;
import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;

public class WorldInteraction {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldInteraction.class);

    private static final float RANGE = 5.0f;

    private long lastLeftClickTime = 0;
    private long lastRightClickTime = 0;

    private static final long CLICK_DELAY_MS = 200; // par exemple 150ms entre deux clics

    private int selectedIndex = 0;

    private final BlockType[] availableBlocks = {
            BlockType.STONE,
            BlockType.GLASS,
            BlockType.LEAVE,
            BlockType.GRASS_BLOCK,
            BlockType.DIRT,
            BlockType.WOOD_LOG,
            BlockType.PLANK,
            BlockType.WATER,
            BlockType.GRASS,
            BlockType.YELLOW_FLOWER
    };

    @Getter
    private HitResult hitResult;

    public void update(PlayerController playerController, InputManager input, World world, Camera camera) {
        hitResult = raycast(world, camera);

        if(hitResult != null && hitResult.blockPos() != null) {
            Statistic.set("Target Block","X : "+hitResult.blockPos().x+" | Y : "+hitResult.blockPos().y+" | Z : "+hitResult.blockPos().z);
        }else{
            Statistic.set("Target Block", "NaN");
        }

        long currentTime = System.currentTimeMillis();

        if (input.isMouseKeyDown(GLFW_MOUSE_BUTTON_LEFT) && currentTime - lastLeftClickTime >= CLICK_DELAY_MS) {
            lastLeftClickTime = currentTime;
            onLeftClick(playerController, world, hitResult);
        }

        if (input.isMouseKeyDown(GLFW_MOUSE_BUTTON_RIGHT) && currentTime - lastRightClickTime >= CLICK_DELAY_MS) {
            lastRightClickTime = currentTime;
            onRightClick(playerController, world, hitResult);
        }

        int scroll = input.getMouseScroll();
        if (scroll != 0) {
            selectedIndex = (selectedIndex - scroll + availableBlocks.length) % availableBlocks.length;
            Statistic.set("Selected", availableBlocks[selectedIndex].name());
        }

    }


    public static HitResult raycast(World world, Camera camera) {
        Vector3f origin = new Vector3f(camera.getPosition());
        Vector3f direction = new Vector3f(camera.getForward()).normalize();

        float step = 0.01f;

        Vector3f currentPos = new Vector3f(origin);
        Vector3i lastBlock = new Vector3i(
                (int) Math.floor(currentPos.x),
                (int) Math.floor(currentPos.y),
                (int) Math.floor(currentPos.z)
        );

        for (float t = 0; t < RANGE; t += step) {
            currentPos.fma(step, direction);

            int x = (int) Math.floor(currentPos.x);
            int y = (int) Math.floor(currentPos.y);
            int z = (int) Math.floor(currentPos.z);
            Vector3i currentBlock = new Vector3i(x, y, z);

            if (!currentBlock.equals(lastBlock)) {
                Chunk chunk = world.getChunkAt(currentPos);
                if (chunk == null) {
                    lastBlock.set(currentBlock);
                    continue;
                }

                short block = chunk.getBlockAtWorld(x, y, z);
                if (block != 0) {
                    Vector3i delta = new Vector3i(currentBlock).sub(lastBlock);

                    FaceDirection face = null;
                    for (FaceDirection dir : FaceDirection.values()) {
                        if (dir.getOffset().equals(delta)) {
                            face = dir.getOpposite();
                            break;
                        }
                    }

                    return new HitResult(currentBlock, face);
                }

                lastBlock.set(currentBlock);
            }
        }

        return null;
    }


    private void onLeftClick(PlayerController playerController, World world, HitResult r) {
        if (r == null) return;

        Vector3i hitBlock = r.blockPos();
        Chunk chunk = world.getChunkAt(new Vector3f(hitBlock));

        if (chunk != null) {
            int localX = hitBlock.x - chunk.getPosition().x * Chunk.SIZE;
            int localY = hitBlock.y - chunk.getPosition().y * Chunk.SIZE;
            int localZ = hitBlock.z - chunk.getPosition().z * Chunk.SIZE;

            if (chunk.isInBounds(localX, localY, localZ)) {
                chunk.setBlock(localX, localY, localZ, BlockType.AIR);
                chunk.setState(ChunkState.DIRTY_NOW);

                world.setDirtyNeighborBlock(chunk, localX, localY, localZ);
            }
        }
    }

    private void onRightClick(PlayerController playerController, World world, HitResult r) {
        if (r == null || r.hitFace() == null) return;

        Vector3i target = new Vector3i(r.blockPos()).add(r.hitFace().getOffset());

        Chunk chunk = world.getChunkAt(new Vector3f(target));
        if (chunk != null) {
            int localX = target.x - chunk.getPosition().x * Chunk.SIZE;
            int localY = target.y - chunk.getPosition().y * Chunk.SIZE;
            int localZ = target.z - chunk.getPosition().z * Chunk.SIZE;

            if (chunk.isInBounds(localX, localY, localZ) && !playerController.isOccupying(target)) {
                chunk.setBlock(localX, localY, localZ, availableBlocks[selectedIndex]);
                chunk.setState(ChunkState.DIRTY_NOW);

                world.setDirtyNeighborBlock(chunk, localX, localY, localZ);
            }
        }
    }



}
