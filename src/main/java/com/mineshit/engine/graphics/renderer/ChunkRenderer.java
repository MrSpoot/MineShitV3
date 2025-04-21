package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.graphics.textures.TextureManager;
import com.mineshit.engine.input.InputManager;
import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.engine.window.Window;
import com.mineshit.game.player.PlayerController;
import com.mineshit.game.world.utils.Chunk;
import com.mineshit.game.world.utils.ChunkRenderable;
import com.mineshit.game.world.utils.ChunkState;
import com.mineshit.game.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.lwjgl.opengl.GL11C.*;

public class ChunkRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkRenderer.class);

    private Shader shader;
    private ShadowRenderer shadowRenderer;

    private final Map<Vector3i, ChunkRenderable> renderables = new HashMap<>();

    public void init() {
        LOGGER.info("Initializing ChunkRenderer");
        shader = new Shader("/shaders/basic.glsl");
        shadowRenderer = new ShadowRenderer(2048,2048);
    }

    public void render(Window window, InputManager input, PlayerController playerController, Camera camera, World world, float alpha) {
        world.getInteraction().update(playerController, input, world, camera);

        shadowRenderer.begin(playerController.getPosition(), world.getClock().getSunDirection());
        renderShadowWorld(world,shadowRenderer.getShader(), shadowRenderer.getLightSpaceMatrix());
        shadowRenderer.end(window.getWidth(), window.getHeight());

        shader.useProgram();
        TextureManager.BLOCK_TEXTURES.bind(0);

        shader.setUniform("uProjection", camera.getProjectionMatrix());
        shader.setUniform("uView", camera.getViewMatrix());
        shader.setUniform("uLightSpaceMatrix", shadowRenderer.getLightSpaceMatrix());
        shader.setUniform("uShadowMap", 1);
        shadowRenderer.bind(1);

        Iterator<Map.Entry<Vector3i, ChunkRenderable>> it = renderables.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ChunkRenderable renderable = entry.getValue();

            if (renderable.getChunk().getState() == ChunkState.DELETED) {
                renderable.cleanup();
                it.remove();
            }
        }

        for (Chunk chunk : world.getChunks(ChunkState.GENERATED, ChunkState.DIRTY, ChunkState.DIRTY_NOW)) {
            Vector3i pos = chunk.getPosition();
            renderables.putIfAbsent(pos, new ChunkRenderable(chunk));
        }

        for(ChunkRenderable renderable : renderables.values().stream().filter(cr -> cr.getChunk().getState().equals(ChunkState.DIRTY_NOW)).toList()) {
            renderable.forceRebuild(world);

            for (FaceDirection dir : FaceDirection.values()) {
                Vector3i neighborChunkPos = new Vector3i(renderable.getChunk().getPosition()).add(dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ());
                ChunkRenderable neighborChunk = renderables.get(neighborChunkPos);
                if(neighborChunk != null) {
                    neighborChunk.forceRebuild(world);
                }
            }
        }

        List<Map.Entry<Vector3i, ChunkRenderable>> sorted = new ArrayList<>(renderables.entrySet());

        sorted.sort(Comparator.comparingDouble(entry -> {
            Vector3i pos = entry.getKey();
            float cx = pos.x * Chunk.SIZE;
            float cy = pos.y * Chunk.SIZE;
            float cz = pos.z * Chunk.SIZE;
            return camera.getPosition().distanceSquared(new Vector3f(cx, cy, cz));
        }));

        for (var entry : sorted) {
            ChunkRenderable renderable = entry.getValue();
            renderable.updateMeshIfNeeded(world);
        }

        renderWorld(world,camera);

        TextureManager.BLOCK_TEXTURES.unbind();
        shader.unbind();
    }

    private void renderShadowWorld(World world, Shader shader, Matrix4f lightSpaceMatrix) {
        for (ChunkRenderable renderable : renderables.values()) {
            renderable.renderShadow(world, shader, lightSpaceMatrix);
        }
    }

    private void renderWorld(World world, Camera camera) {
        for (ChunkRenderable renderable : renderables.values()) {
            renderable.renderOpaque(world,shader);
        }

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);

        List<Map.Entry<Vector3i, ChunkRenderable>> sortedTransparent = renderables.entrySet()
                .stream()
                .filter(entry -> entry.getValue().hasTransparent())
                .sorted(Comparator.comparingDouble(entry -> {
                    Vector3i pos = entry.getKey();
                    float cx = pos.x * Chunk.SIZE;
                    float cy = pos.y * Chunk.SIZE;
                    float cz = pos.z * Chunk.SIZE;
                    return -camera.getPosition().distanceSquared(new Vector3f(cx, cy, cz));
                }))
                .toList();


        for (var entry : sortedTransparent) {
            ChunkRenderable renderable = entry.getValue();
            renderable.renderTransparent(world, shader);
        }

        glDepthMask(true);
        glDisable(GL_BLEND);
    }

    public void cleanup() {
        shader.destroy();
        for (ChunkRenderable renderable : renderables.values()) {
            renderable.cleanup();
        }
        ChunkRenderable.cleanupStatic();
    }
}
