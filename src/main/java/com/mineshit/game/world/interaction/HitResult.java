package com.mineshit.game.world.interaction;

import com.mineshit.engine.utils.FaceDirection;
import org.joml.Vector3i;

public record HitResult(Vector3i blockPos, FaceDirection hitFace) {
}
