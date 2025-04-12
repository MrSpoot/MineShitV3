package com.mineshit.engine.input;

import lombok.Getter;
import org.lwjgl.glfw.GLFWScrollCallback;

import static org.lwjgl.glfw.GLFW.*;

public class InputManager {

    private final long windowId;

    private double mouseX, mouseY;
    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;

    @Getter
    private float mouseDeltaX, mouseDeltaY;

    private final GLFWScrollCallback scrollCallback;

    private double scrollOffsetY = 0;

    public InputManager(long windowId) {
        this.windowId = windowId;

        scrollCallback = glfwSetScrollCallback(windowId, (window, xoffset, yoffset) -> {
            scrollOffsetY += yoffset;
        });
    }

    public void update() {
        // Souris
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(windowId, x, y);

        mouseX = x[0];
        mouseY = y[0];

        if (firstMouse) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            firstMouse = false;
        }

        mouseDeltaX = (float) (mouseX - lastMouseX);
        mouseDeltaY = (float) (lastMouseY - mouseY);

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public boolean isKeyDown(int key) {
        return glfwGetKey(windowId, key) == GLFW_PRESS;
    }

    public boolean isMouseKeyDown(int key) {
        return glfwGetMouseButton(windowId, key) == GLFW_PRESS;
    }

    public int getMouseScroll() {
        int scroll = (int) scrollOffsetY;
        scrollOffsetY -= scroll; // Réinitialise le scroll utilisé
        return scroll;
    }

    public void cleanup(){
        if (scrollCallback != null) scrollCallback.close();
    }

}
