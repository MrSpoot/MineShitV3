package com.mineshit.engine.graphics.textures;

import com.mineshit.Game;
import com.mineshit.engine.utils.FileReader;
import com.mineshit.engine.utils.Image;
import com.mineshit.game.world.BlockType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class TextureLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextureLoader.class);

    public static TextureArray loadBlockTextureArray() {
        List<ByteBuffer> textures = new ArrayList<>();

        int width = -1, height = -1;

        for (BlockType type : BlockType.values()) {
            if (type.getTexturePath() == null) continue;

            LOGGER.debug("Loading texture at {}", type.getTexturePath());

            Image image = FileReader.readImage(type.getTexturePath(), true);
            if (image == null) continue;

            if (width == -1) {
                width = image.getWidth();
                height = image.getHeight();
            }

            textures.add(image.getByteBuffer());
        }

        LOGGER.info("Loading {} textures", textures.size());

        return new TextureArray(width, height, textures);
    }
}

