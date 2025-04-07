package com.mineshit.engine.utils;

import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector3i;

@Getter
public enum FaceDirection {
    BACK(0, 0, -1),   // Z-
    FRONT(0, 0, 1),   // Z+
    LEFT(-1, 0, 0),   // X-
    RIGHT(1, 0, 0),   // X+
    BOTTOM(0, -1, 0), // Y-
    TOP(0, 1, 0);     // Y+

    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    FaceDirection(int offsetX, int offsetY, int offsetZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public Vector3i getOffset() {
        return new Vector3i(offsetX, offsetY, offsetZ);
    }

    public FaceDirection getOpposite() {
        return switch (this) {
            case FRONT -> BACK;
            case BACK -> FRONT;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            case TOP -> BOTTOM;
            case BOTTOM -> TOP;
        };
    }

    public static FaceDirection fromHit(Vector3f hitPos, Vector3i blockPos) {
        float dx = hitPos.x - blockPos.x;
        float dy = hitPos.y - blockPos.y;
        float dz = hitPos.z - blockPos.z;

        float absX = Math.abs(dx - 0.5f);
        float absY = Math.abs(dy - 0.5f);
        float absZ = Math.abs(dz - 0.5f);

        if (absX > absY && absX > absZ) {
            return dx > 0.5f ? RIGHT : LEFT;
        } else if (absY > absX && absY > absZ) {
            return dy > 0.5f ? TOP : BOTTOM;
        } else {
            return dz > 0.5f ? FRONT : BACK;
        }
    }

}

