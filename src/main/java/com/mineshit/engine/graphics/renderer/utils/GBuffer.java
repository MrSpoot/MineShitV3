package com.mineshit.engine.graphics.renderer.utils;

import lombok.Getter;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.EnumMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;

public class GBuffer {

    public enum Attachment {
        ALBEDO,
        NORMAL,
        POSITION
    }

    @Getter private final int width, height;

    @Getter
    private final int fbo;
    @Getter
    private final int depthTexture;
    private final Map<Attachment, Integer> textures = new EnumMap<>(Attachment.class);

    public GBuffer(int width, int height) {
        this.width = width;
        this.height = height;

        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        addColorAttachment(Attachment.ALBEDO, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE);
        addColorAttachment(Attachment.NORMAL, GL_RGB16F, GL_RGB, GL_FLOAT);
        addColorAttachment(Attachment.POSITION, GL_RGB16F, GL_RGB, GL_FLOAT);

        depthTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0,
                GL_DEPTH_COMPONENT, GL_FLOAT, (IntBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer drawBuffers = stack.mallocInt(textures.size());
            textures.keySet().forEach(att -> drawBuffers.put(GL_COLOR_ATTACHMENT0 + att.ordinal()));
            drawBuffers.flip();
            glDrawBuffers(drawBuffers);
        }

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("GBuffer framebuffer is not complete!");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void addColorAttachment(Attachment attachment, int internalFormat, int format, int type) {
        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + attachment.ordinal(), GL_TEXTURE_2D, tex, 0);

        textures.put(attachment, tex);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void unbind(int screenWidth, int screenHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenWidth, screenHeight);
    }

    public void bindTexture(Attachment attachment, int unit) {
        Integer tex = textures.get(attachment);
        if (tex != null) {
            glActiveTexture(GL_TEXTURE0 + unit);
            glBindTexture(GL_TEXTURE_2D, tex);
        }
    }

    public void bindDepthTexture(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
    }

    public void cleanup() {
        textures.values().forEach(GL30::glDeleteTextures);
        glDeleteTextures(depthTexture);
        glDeleteFramebuffers(fbo);
    }
}
