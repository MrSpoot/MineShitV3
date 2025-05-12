package com.mineshit.engine.graphics.renderer.utils;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.graphics.renderer.passes.RenderPass;
import com.mineshit.engine.window.Window;
import com.mineshit.game.player.PlayerController;
import com.mineshit.game.world.World;
import com.mineshit.game.world.utils.ChunkRenderable;
import org.joml.Matrix4f;
import java.util.Collection;
import java.util.List;

public record RenderContext(
        Window window,
        World world,
        Camera camera,
        PlayerController player,
        Matrix4f lightMatrix,
        Collection<ChunkRenderable> renderables,
        GBuffer gbuffer,
        ShadowMap shadowMap,
        SsaoMap ssaoMap,
        LightingMap lightingMap,
        SkyboxMap skyboxMap
) {}
