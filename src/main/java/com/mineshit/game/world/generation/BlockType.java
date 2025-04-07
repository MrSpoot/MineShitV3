package com.mineshit.game.world.generation;

import lombok.Getter;

@Getter
public enum BlockType {
    AIR(null),
    GRASS("/textures/grass_full.png"),
    DIRT("/textures/dirt_full.png"),
    STONE("/textures/stone_full.png"),
    TEST("/textures/test_full.png");

    private final String texturePath;

    BlockType(String path) {
        this.texturePath = path;
    }

    public short getId() {
        return (short) this.ordinal();
    }
}
