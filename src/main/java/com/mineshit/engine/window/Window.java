package com.mineshit.engine.window;

import lombok.Getter;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private static final Logger LOGGER = LoggerFactory.getLogger(Window.class);

    @Getter
    private long id;
    private String title;
    @Getter
    private int width, height;

    private GLFWFramebufferSizeCallback resizeCallback;
    private GLFWErrorCallback errorCallback;

    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public float aspectRatio() {
        return width / (float) height;
    }

    public boolean shouldClose(){
        return glfwWindowShouldClose(this.id);
    }

    public void create(){
        LOGGER.info("Creating window");
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        glfwWindowHint(GLFW_SAMPLES, 4);

        //TODO Make better code for fullscreen
        if (this.width == 0 && this.height == 0) {
            glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            assert vidMode != null;
            width = vidMode.width();
            height = vidMode.height();
        }

        id = glfwCreateWindow(width, height, title, NULL, NULL);
        if (id == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        resizeCallback = new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                glViewport(0, 0, width, height);
                Window.this.width = width;
                Window.this.height = height;
            }
        };
        glfwSetFramebufferSizeCallback(id, resizeCallback);

        errorCallback = GLFWErrorCallback.create((errorCode, msgPtr) -> {
            LOGGER.error("GLFW Error [{}]: {}", errorCode, MemoryUtil.memUTF8(msgPtr));
        });
        glfwSetErrorCallback(errorCallback);

        glfwMakeContextCurrent(id);
        glfwSetInputMode(id,GLFW_CURSOR,GLFW_CURSOR_DISABLED);
        GL.createCapabilities();

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        glEnable(GL_DEPTH_TEST);

        glfwSwapInterval(0);
        glViewport(0,0,width,height);
        glfwShowWindow(id);
    }

    public void update(){
        glfwSwapBuffers(id);
        glfwPollEvents();
    }

    public void close(){
        glfwSetWindowShouldClose(id, true);
    }

    public void cleanup() {
        if (resizeCallback != null) resizeCallback.close();
        if (errorCallback != null) errorCallback.close();
        glfwDestroyWindow(id);
        glfwTerminate();
    }

}
