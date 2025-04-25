package com.mineshit.engine.graphics.renderer.utils;

import lombok.Getter;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL30C.*;

public class GBuffer {

    public enum Attachment {
        ALBEDO,
        NORMAL,
        POSITION
    }

    @Getter
    private final int width, height;

    private final int fbo;
    private final int depthRenderbuffer;

    private final Map<Attachment, Integer> textures = new EnumMap<>(Attachment.class);

    public GBuffer(int width, int height) {
        this.width = width;
        this.height = height;

        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        // Attachments
        addColorAttachment(Attachment.ALBEDO, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE);
        addColorAttachment(Attachment.NORMAL, GL_RGB16F, GL_RGB, GL_FLOAT);
        addColorAttachment(Attachment.POSITION, GL_RGB16F, GL_RGB, GL_FLOAT);

        // Depth
        depthRenderbuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthRenderbuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderbuffer);

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

    public void cleanup() {
        textures.values().forEach(GL30::glDeleteTextures);
        glDeleteRenderbuffers(depthRenderbuffer);
        glDeleteFramebuffers(fbo);
    }
}
