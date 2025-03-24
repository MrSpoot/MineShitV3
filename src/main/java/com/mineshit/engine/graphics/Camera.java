package com.mineshit.engine.graphics;

import com.mineshit.engine.window.Window;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Camera {

    private static final Logger LOGGER = LoggerFactory.getLogger(Camera.class);

    @Getter
    private final float fov;
    @Setter
    private float aspectRatio;
    private final float near = 0.1f;
    private final float far = 1000f;

    private final Vector3f position = new Vector3f();
    private float pitch = 0; // top/bot
    private float yaw = -90; // left/right

    public Camera(float fov, float aspectRatio) {
        this.aspectRatio = aspectRatio;
        this.fov = fov;
        LOGGER.debug("Camera created. FOV: {} - Aspect Ratio: {}", this.fov, this.aspectRatio);
    }

    public void move(Vector3f offset) {
        position.add(offset);
    }

    public void rotate(float yawDelta, float pitchDelta) {
        yaw += yawDelta;
        pitch += pitchDelta;
        pitch = Math.max(-89f, Math.min(89f, pitch));
    }

    public void moveRelative(Vector3f localOffset) {
        Vector3f forward = getForward();
        Vector3f right = new Vector3f(forward).cross(new Vector3f(0, 1, 0)).normalize();

        position.add(new Vector3f(forward).mul(localOffset.z));
        position.add(new Vector3f(right).mul(localOffset.x));
        position.y += localOffset.y;
    }


    private Vector3f getForward() {
        return new Vector3f(
                (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))),
                (float) Math.sin(Math.toRadians(pitch)),
                (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)))
        ).normalize();
    }


    public Vector3f getTarget() {
        return new Vector3f(position).add(getForward());
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, getTarget(), new Vector3f(0, 1, 0));
    }

    public Matrix4f getProjectionMatrix() {
        return new Matrix4f().perspective(
                (float) Math.toRadians(fov),
                aspectRatio,
                near,
                far
        );
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }
}
