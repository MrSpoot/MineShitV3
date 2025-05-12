package com.mineshit.engine.graphics.renderer.utils;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;

public class SkyboxMap {

    private final int fbo;
    private final Texture texture;

    public SkyboxMap(int width, int height) {
        fbo = glGenFramebuffers();
        texture = new Texture(width, height, GL_RGB16F, GL_RGB, GL_FLOAT);

        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        texture.attach(GL_COLOR_ATTACHMENT0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Failed to create Skybox framebuffer");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, texture.getWidth(), texture.getHeight());
        glClear(GL_COLOR_BUFFER_BIT);
    }

    public void unbind(int screenWidth, int screenHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenWidth, screenHeight);
    }

    public Texture getTexture() {
        return texture;
    }

    public void cleanup() {
        glDeleteFramebuffers(fbo);
        texture.cleanup();
    }
}

