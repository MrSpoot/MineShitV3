package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.graphics.textures.TextureManager;
import lombok.Getter;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShadowRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShadowRenderer.class);

    @Getter
    private final Shader shader;
    @Getter
    private final ShadowMap shadowMap;
    @Getter
    private final Matrix4f lightSpaceMatrix;

    public ShadowRenderer(int width, int height) {
        LOGGER.info("Initializing ShadowRenderer");
        this.shader = new Shader("/shaders/shadow.glsl");
        this.shadowMap = new ShadowMap(width, height);
        this.lightSpaceMatrix = new Matrix4f();
    }

    public void begin(Vector3f playerPos, Vector3f lightDir) {
        shader.useProgram();

        TextureManager.BLOCK_TEXTURES.bind(0);
        shader.setUniform("uTextureArray", 0);

        // Paramètres de la lumière
        Vector3f lightDirection = new Vector3f(0.89f, -0.45f, 0f).normalize();
        Vector3f playerPosition = new Vector3f(-37.5f, 14.91f, -7.445f); // à adapter à ta scène

// Distance depuis le joueur vers la lumière
        float lightDistance = 100f;
        Vector3f lightPos = new Vector3f(playerPosition).fma(-lightDistance, lightDirection);

// Paramètres de la projection
        float orthoSize = 50f; // ← essaie 50, 100 ou plus selon ta scène
        float nearPlane = 1f;
        float farPlane = 200f;

// Matrices de projection et de vue pour la lumière
        Matrix4f lightProjection = new Matrix4f().ortho(
                -orthoSize, orthoSize,
                -orthoSize, orthoSize,
                nearPlane, farPlane
        );

        Matrix4f lightView = new Matrix4f().lookAt(
                lightPos,
                playerPosition,                // on regarde vers le centre de la scène
                new Vector3f(0f, 1f, 0f)       // up vector
        );

// lightSpaceMatrix = projection * vue
        Matrix4f lightSpaceMatrix = new Matrix4f();
        lightProjection.mul(lightView, lightSpaceMatrix);

        LOGGER.debug("ShadowRenderer - Player position: {}", playerPos);
        LOGGER.debug("ShadowRenderer - Light direction: {}", lightDir);
        LOGGER.debug("ShadowRenderer - Computed light position: {}", lightPos);

        lightProjection.mul(lightView, lightSpaceMatrix);

        LOGGER.debug("ShadowRenderer - Light view matrix:\n{}", lightView);
        LOGGER.debug("ShadowRenderer - Light projection matrix:\n{}", lightProjection);
        LOGGER.debug("ShadowRenderer - Final lightSpaceMatrix:\n{}", lightSpaceMatrix);

        shadowMap.bind();
    }


    public void end(int screenWidth, int screenHeight) {
        shadowMap.unbind(screenWidth, screenHeight);
        shader.unbind();
    }

    public void bind(int unit) {
        shadowMap.bindTexture(unit);
    }

}
