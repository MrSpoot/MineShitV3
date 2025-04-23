package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.graphics.textures.TextureManager;
import lombok.Getter;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11C.*;

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
        updateLightSpaceMatrix(playerPos, lightDir);

        glDisable(GL_CULL_FACE);

        shadowMap.bind();

        shader.useProgram();

        TextureManager.BLOCK_TEXTURES.bind(0);
        shader.setUniform("uTextureArray", 0);
        shader.setUniform("uLightSpaceMatrix", lightSpaceMatrix);
    }

    public void end(int screenWidth, int screenHeight) {
        shader.unbind();
        shadowMap.unbind(screenWidth, screenHeight);
        glEnable(GL_CULL_FACE);
    }

    public void bind(int unit) {
        shadowMap.bindTexture(unit);
    }

    private void updateLightSpaceMatrix(Vector3f playerPos, Vector3f lightDir) {
        Vector3f lightDirection = new Vector3f(lightDir).normalize();

        // 🔁 Snap à une grille pour éviter que les ombres bougent à chaque petit déplacement
        float snapSize = 32.0f;

        Vector3f center = new Vector3f(playerPos);
        center.x = (float) Math.floor(center.x / snapSize) * snapSize;
        center.y = (float) Math.floor(center.y / snapSize) * snapSize;
        center.z = (float) Math.floor(center.z / snapSize) * snapSize;

        // 💡 Position de la lumière éloignée dans la direction opposée
        float lightDistance = 50f;
        Vector3f lightPos = new Vector3f(center).fma(-lightDistance, lightDirection);

        // 🎥 Vue orthographique : zone visible pour les ombres
        float orthoSize = 50.0f; // taille du carré vu par la lumière
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
