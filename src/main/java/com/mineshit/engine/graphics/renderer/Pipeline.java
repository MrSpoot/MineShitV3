package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.graphics.renderer.passes.RenderPass;
import com.mineshit.engine.graphics.renderer.utils.ChunkMeshUpdater;
import com.mineshit.engine.graphics.renderer.utils.GBuffer;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.ShadowMap;
import com.mineshit.engine.input.InputManager;
import com.mineshit.engine.window.Window;
import com.mineshit.game.player.PlayerController;
import com.mineshit.game.world.World;
import com.mineshit.game.world.utils.ChunkRenderable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Pipeline {
    private final List<RenderPass> passes = new LinkedList<>();

    private Matrix4f lightSpaceMatrix;
    private final Map<Vector3i, ChunkRenderable> renderables = new HashMap<>();
    private GBuffer gBuffer;
    private ShadowMap shadowMap;

    public void init(Window window) {

        this.lightSpaceMatrix = new Matrix4f();
        this.gBuffer = new GBuffer(window.getWidth(), window.getHeight());
        this.shadowMap = new ShadowMap(4096,4096);

        passes.forEach(RenderPass::init);
    }

    public void render(Window window,
                       InputManager input,
                       World world,
                       Camera camera,
                       PlayerController player) {

        if(window.getWidth() != gBuffer.getWidth() || window.getHeight() != gBuffer.getHeight()) {
            this.gBuffer.cleanup();
            this.gBuffer = new GBuffer(window.getWidth(), window.getHeight());
        }

        world.getInteraction().update(player, input, world, camera);
        updateLightSpaceMatrix(player.getPosition(),world.getClock().getSunDirection());

        ChunkMeshUpdater.update(renderables,world,camera);

        RenderContext ctx = new RenderContext(window,world,camera, player, lightSpaceMatrix, renderables.values(), gBuffer, shadowMap);

        for (RenderPass pass : passes) {
            pass.render(ctx);
        }
    }

    public void cleanup() {
        this.gBuffer.cleanup();
        this.shadowMap.cleanup();
        passes.forEach(RenderPass::cleanup);

        for(ChunkRenderable chunk : renderables.values()) {
            chunk.cleanup();
        }

        ChunkRenderable.cleanupStatic();
    }

    public void addPass(RenderPass pass) {
        passes.add(pass);
    }

    private void updateLightSpaceMatrix(Vector3f playerPos, Vector3f lightDir) {
        Vector3f lightDirection = new Vector3f(lightDir).normalize();
        float snapSize = 32.0f;

        Vector3f center = new Vector3f(playerPos);
        center.x = (float) Math.floor(center.x / snapSize) * snapSize;
        center.y = (float) Math.floor(center.y / snapSize) * snapSize;
        center.z = (float) Math.floor(center.z / snapSize) * snapSize;

        float lightDistance = 50f;
        Vector3f lightPos = new Vector3f(center).fma(-lightDistance, lightDirection);

        float orthoSize = 50.0f;
        float nearPlane = 0.1f;
        float farPlane = 200.0f;

        Matrix4f lightProjection = new Matrix4f().ortho(
                -orthoSize, orthoSize,
                -orthoSize, orthoSize,
                nearPlane, farPlane
        );

        Matrix4f lightView = new Matrix4f().lookAt(
                lightPos,
                center,
                new Vector3f(0f, 1f, 0f)
        );

        this.lightSpaceMatrix.set(lightProjection).mul(lightView);
    }
}

