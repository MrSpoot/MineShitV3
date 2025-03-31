package com.mineshit.engine.graphics.textures;

public class TextureManager {
    public static TextureArray BLOCK_TEXTURES;

    public static void init() {
        BLOCK_TEXTURES = TextureLoader.loadBlockTextureArray();
    }
}
