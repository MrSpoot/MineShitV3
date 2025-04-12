package com.mineshit.game.world.generation;

import com.mineshit.engine.utils.CullingMode;
import lombok.Getter;

@Getter
public enum BlockType {
    AIR(null,0.0f, true, CullingMode.NONE),
    GRASS("/textures/grass_full.png",1.0f,false, CullingMode.CULL_IF_OPAQUE),
    DIRT("/textures/dirt_full.png",1.0f,false, CullingMode.CULL_IF_OPAQUE),
    STONE("/textures/stone_full.png",1.0f,false, CullingMode.CULL_IF_OPAQUE),
    SAND("/textures/sand_full.png",1.0f,false, CullingMode.CULL_IF_OPAQUE),
    GLASS("/textures/glass_full.png",1.0f,true, CullingMode.CULL_IF_SAME),
    WOOD_LOG("/textures/wood_log_full.png",1.0f,false, CullingMode.CULL_IF_OPAQUE),
    LEAVE("/textures/leave_full.png",0.5f,true, CullingMode.CULL_IF_OPAQUE),
    WATER("/textures/water_full.png",0.3f,true, CullingMode.CULL_IF_SAME),
    TEST("/textures/test_full.png",1.0f,false, CullingMode.CULL_IF_OPAQUE);

    private final String texturePath;
    private final float density;
    private final boolean isTransparent;
    private final CullingMode cullingMode;

    BlockType(String path, float density, boolean isTransparent, CullingMode cullingMode) {
        this.texturePath = path;
        this.density = density;
        this.isTransparent = isTransparent;
        this.cullingMode = cullingMode;
    }

    public short getId() {
        return (short) this.ordinal();
    }

    public boolean isTransparent() {
        return this.isTransparent;
    }

    public boolean isSolid() {
        return density >= 1.0f;
    }

    public static BlockType fromId(short id) {
        for (BlockType blockType : BlockType.values()) {
            if (blockType.getId() == id) {
                return blockType;
            }
        }
        return null;
    }
}
