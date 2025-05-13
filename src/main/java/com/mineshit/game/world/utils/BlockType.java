package com.mineshit.game.world.utils;

import com.mineshit.engine.utils.CullingMode;
import lombok.Getter;

@Getter
public enum BlockType {
    AIR(null, 0.0f, MeshType.BLOCK, TransparencyType.TRANSPARENT, CullingMode.NONE),
    GRASS_BLOCK("/textures/grass_block_full.png", 1.0f,MeshType.BLOCK, TransparencyType.OPAQUE, CullingMode.CULL_IF_OPAQUE),
    DIRT("/textures/dirt_full.png", 1.0f,MeshType.BLOCK, TransparencyType.OPAQUE, CullingMode.CULL_IF_OPAQUE),
    STONE("/textures/stone_full.png", 1.0f,MeshType.BLOCK, TransparencyType.OPAQUE, CullingMode.CULL_IF_OPAQUE),
    SAND("/textures/sand_full.png", 1.0f,MeshType.BLOCK, TransparencyType.OPAQUE, CullingMode.CULL_IF_OPAQUE),
    GLASS("/textures/glass_full.png", 1.0f,MeshType.BLOCK, TransparencyType.TRANSLUCENT, CullingMode.CULL_IF_SAME),
    WOOD_LOG("/textures/wood_log_full.png", 1.0f,MeshType.BLOCK, TransparencyType.OPAQUE, CullingMode.CULL_IF_OPAQUE),
    PLANK("/textures/plank_full.png", 1.0f,MeshType.BLOCK, TransparencyType.OPAQUE, CullingMode.CULL_IF_OPAQUE),
    LEAVE("/textures/leave_full.png", 1.0f,MeshType.BLOCK, TransparencyType.CUTOUT, CullingMode.CULL_IF_OPAQUE),
    WATER("/textures/water_full.png", 1.0f,MeshType.BLOCK, TransparencyType.TRANSLUCENT, CullingMode.CULL_IF_SAME),
    GRASS("/textures/grass_full.png", 0.0f,MeshType.CROSS, TransparencyType.TRANSLUCENT, CullingMode.NONE),
    YELLOW_FLOWER("/textures/yellow_flower_full.png", 0.0f,MeshType.CROSS, TransparencyType.TRANSLUCENT, CullingMode.NONE),
    TEST("/textures/test_full.png", 1.0f,MeshType.BLOCK, TransparencyType.OPAQUE, CullingMode.CULL_IF_OPAQUE);


    private final String texturePath;
    private final float density;
    private final MeshType meshType;
    private final TransparencyType transparencyType;
    private final CullingMode cullingMode;

    BlockType(String path, float density, MeshType meshType, TransparencyType transparencyType, CullingMode cullingMode) {
        this.texturePath = path;
        this.density = density;
        this.meshType = meshType;
        this.transparencyType = transparencyType;
        this.cullingMode = cullingMode;
    }

    public short getId() {
        return (short) this.ordinal();
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
