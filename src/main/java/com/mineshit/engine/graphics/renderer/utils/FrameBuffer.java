package com.mineshit.engine.graphics.renderer.utils;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_NEAREST;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.glTexParameteri;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL14C.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL20C.glDrawBuffers;
import static org.lwjgl.opengl.GL30C.*;

public class FrameBuffer {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrameBuffer.class);

    @Getter private final int width;
    @Getter private final int height;
    @Getter private final int fbo;
    @Getter private final int depthTexture;
    @Getter private final int albedoTexture;
    @Getter private final int normalTexture;
    @Getter private final int positionTexture;

    public FrameBuffer(int width, int height) {
        this.width = width;
        this.height = height;

        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        //DEPTH
        depthTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0,
                GL_DEPTH_COMPONENT, GL_FLOAT, (IntBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);

        //ALBEDO
        albedoTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, albedoTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, albedoTexture, 0);

        //NORMAL
        normalTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, normalTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width, height, 0, GL_RGB, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, normalTexture, 0);

        //POSITION
        positionTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, positionTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width, height, 0, GL_RGB, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, positionTexture, 0);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer drawBuffers = stack.mallocInt(3);
            drawBuffers.put(GL_COLOR_ATTACHMENT0);
            drawBuffers.put(GL_COLOR_ATTACHMENT1);
            drawBuffers.put(GL_COLOR_ATTACHMENT2);
            drawBuffers.flip();
            glDrawBuffers(drawBuffers);
        }

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("FrameBuffer framebuffer is not complete!");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public static void bindTexture(int textureId, int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
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

    public void cleanup(){
        glDeleteTextures(depthTexture);
        glDeleteTextures(albedoTexture);
        glDeleteTextures(normalTexture);
        glDeleteTextures(positionTexture);
        glDeleteFramebuffers(fbo);
    }


}
