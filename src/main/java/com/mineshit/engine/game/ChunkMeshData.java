package com.mineshit.engine.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public record ChunkMeshData(
        FloatBuffer opaqueVertexBuffer,
        IntBuffer opaqueIndexBuffer,
        int opaqueVertexCount,

        FloatBuffer transparentVertexBuffer,
        IntBuffer transparentIndexBuffer,
        int transparentVertexCount,

        FloatBuffer shadowVertexBuffer,
        IntBuffer shadowIndexBuffer,
        int shadowVertexCount
) {

    public boolean hasOpaque() {
        return opaqueVertexCount > 0;
    }

    public boolean hasTransparent() {
        return transparentVertexCount > 0;
    }

    public boolean canBeAdd() {
        return hasOpaque() || hasTransparent();
    }
}

