package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.graphics.renderer.passes.InterfacePass;
import com.mineshit.engine.graphics.renderer.passes.RenderPass;
import com.mineshit.engine.graphics.renderer.utils.*;
import com.mineshit.engine.input.InputManager;
import com.mineshit.engine.window.Window;
import com.mineshit.game.player.PlayerController;
import com.mineshit.game.world.World;
import com.mineshit.game.world.utils.ChunkRenderable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import static org.lwjgl.glfw.GLFW.*;

import java.util.*;

public class Pipeline {
    private final List<RenderPass> passes = new LinkedList<>();

    private Matrix4f lightSpaceMatrix;
    private final Map<Vector3i, ChunkRenderable> renderables = new HashMap<>();
    private ShadowMap shadowMap;
    private SsaoMap ssaoMap;
    private LightingMap lightingMap;
    private SkyboxMap skyboxMap;
    private GBuffer gbuffer;

    //TODO TEMPORARY
    private boolean showInterface = true;
    private double lastToggleTime = 0;
    private static final double TOGGLE_COOLDOWN = 0.2; // en secondes

    public void init(Window window) {

        this.lightSpaceMatrix = new Matrix4f();
        this.shadowMap = new ShadowMap(4096,4096);
        this.ssaoMap= new SsaoMap(window.getWidth(), window.getHeight());
        this.lightingMap = new LightingMap(window.getWidth(), window.getHeight());
        this.skyboxMap = new SkyboxMap(window.getWidth(), window.getHeight());
        this.gbuffer = new GBuffer(window.getWidth(), window.getHeight());

        passes.forEach(pass -> pass.init(window));
    }

    public void render(Window window,
                       InputManager input,
                       World world,
                       Camera camera,
                       PlayerController player) {

        if (gbuffer.getWidth() != window.getWidth() || gbuffer.getHeight() != window.getHeight()) {
            gbuffer.cleanup();
            gbuffer = new GBuffer(window.getWidth(), window.getHeight());

            ssaoMap.cleanup();
            ssaoMap= new SsaoMap(window.getWidth(), window.getHeight());

            lightingMap.cleanup();
            lightingMap= new LightingMap(window.getWidth(), window.getHeight());

            skyboxMap.cleanup();
            skyboxMap= new SkyboxMap(window.getWidth(), window.getHeight());
        }

        double currentTime = System.nanoTime() / 1e9; // en secondes
        if (input.isKeyDown(GLFW_KEY_F1) && (currentTime - lastToggleTime) > TOGGLE_COOLDOWN) {
            showInterface = !showInterface;
            lastToggleTime = currentTime;
        }


        world.getInteraction().update(player, input, world, camera);
        updateLightSpaceMatrix(player.getPosition(),world.getClock().getSunDirection());

        ChunkMeshUpdater.update(renderables,world,camera);

        RenderContext ctx = new RenderContext(window,world,camera, player, lightSpaceMatrix,renderables.values(),gbuffer, shadowMap,ssaoMap,lightingMap,skyboxMap);

        for (RenderPass pass : passes) {
            if(pass instanceof InterfacePass){
                if(!showInterface) {
                    continue;
                }
            }
            pass.render(ctx);
        }
    }

    public void cleanup() {
        this.shadowMap.cleanup();
        this.gbuffer.cleanup();
        this.ssaoMap.cleanup();
        this.lightingMap.cleanup();
        passes.forEach(RenderPass::cleanup);

        for (ChunkRenderable chunk : renderables.values()) {
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

