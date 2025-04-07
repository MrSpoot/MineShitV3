package com.mineshit.game.world.interaction;

import com.mineshit.engine.graphics.Camera;
import com.mineshit.engine.utils.FaceDirection;
import com.mineshit.game.world.World;
import com.mineshit.game.world.generation.Chunk;
import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldInteraction {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldInteraction.class);

    private static final float RANGE = 5.0f;

    @Getter
    private HitResult currentTarget;

    public void update(World world, Camera camera){
        this.currentTarget = raycast(world, camera);
    }

    public HitResult raycast(World world, Camera camera) {
        Vector3f origin = camera.getPosition();
        Vector3f rayDir = camera.getForward();

        Vector3i currentBlock = new Vector3i(
                (int) Math.floor(origin.x),
                (int) Math.floor(origin.y),
                (int) Math.floor(origin.z)
        );

        Vector3i step = new Vector3i(
                Integer.signum((int) rayDir.x),
                Integer.signum((int) rayDir.y),
                Integer.signum((int) rayDir.z)
        );

        Vector3f deltaDist = new Vector3f(
                rayDir.x == 0 ? Float.MAX_VALUE : Math.abs(1.0f / rayDir.x),
                rayDir.y == 0 ? Float.MAX_VALUE : Math.abs(1.0f / rayDir.y),
                rayDir.z == 0 ? Float.MAX_VALUE : Math.abs(1.0f / rayDir.z)
        );

        Vector3f rayOriginBlock = new Vector3f(
                origin.x - (float) Math.floor(origin.x),
                origin.y - (float) Math.floor(origin.y),
                origin.z - (float) Math.floor(origin.z)
        );

        Vector3f sideDist = new Vector3f(
                (step.x > 0 ? (1.0f - rayOriginBlock.x) : rayOriginBlock.x) * deltaDist.x,
                (step.y > 0 ? (1.0f - rayOriginBlock.y) : rayOriginBlock.y) * deltaDist.y,
                (step.z > 0 ? (1.0f - rayOriginBlock.z) : rayOriginBlock.z) * deltaDist.z
        );


        float t = 0f;
        FaceDirection lastHit = null;

        while (t <= RANGE) {
            Chunk chunk = world.getChunkAt(new Vector3f(currentBlock));
            if (chunk != null && chunk.isInBounds((int) (double) currentBlock.x,(int) (double) currentBlock.y,(int) (double) currentBlock.z) && chunk.getBlock((int) (double) currentBlock.x,(int) (double) currentBlock.y,(int) (double) currentBlock.z) != 0) {
                return new HitResult(new Vector3i((int) (double) currentBlock.x,(int) (double) currentBlock.y,(int) (double) currentBlock.z), lastHit);
            }

            if (sideDist.x < sideDist.y && sideDist.x < sideDist.z) {
                currentBlock.x += step.x;
                t = sideDist.x;
                sideDist.x += deltaDist.x;
                lastHit = (step.x > 0) ? FaceDirection.LEFT : FaceDirection.RIGHT;
            } else if (sideDist.y < sideDist.z) {
                currentBlock.y += step.y;
                t = sideDist.y;
                sideDist.y += deltaDist.y;
                lastHit = (step.y > 0) ? FaceDirection.BOTTOM : FaceDirection.TOP;
            } else {
                currentBlock.z += step.z;
                t = sideDist.z;
                sideDist.z += deltaDist.z;
                lastHit = (step.z > 0) ? FaceDirection.BACK : FaceDirection.FRONT;
            }
        }

        return null; // rien touché
    }

}
