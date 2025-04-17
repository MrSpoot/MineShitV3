package com.mineshit.engine.graphics.textures;

import com.mineshit.engine.graphics.renderer.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextureManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextureManager.class);
    public static TextureArray BLOCK_TEXTURES;

    public static void init() {
        LOGGER.info("Load Texture");
        BLOCK_TEXTURES = TextureLoader.loadBlockTextureArray();
    }
}
