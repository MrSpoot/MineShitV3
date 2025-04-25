package com.mineshit.engine.graphics.renderer.utils;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.window.Window;
import com.mineshit.game.player.PlayerController;
import com.mineshit.game.world.World;
import com.mineshit.game.world.utils.ChunkRenderable;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public record RenderContext(
        Window window,
        World world,
        Camera camera,
        PlayerController player,
        Matrix4f lightMatrix,
        Collection<ChunkRenderable> renderables,
        GBuffer gBuffer,
        ShadowMap shadowMap
) {}
