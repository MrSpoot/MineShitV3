package com.mineshit.game.world;

import lombok.Getter;

@Getter
public enum BlockType {
    AIR(null),
    GRASS("/textures/grass.png"),
    DIRT("/textures/dirt.png"),
    STONE("/textures/stone.png");

    private final String texturePath;

    BlockType(String path) {
        this.texturePath = path;
    }
}
