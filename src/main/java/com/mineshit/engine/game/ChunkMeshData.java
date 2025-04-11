package com.mineshit.engine.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public record ChunkMeshData(FloatBuffer vertexBuffer, IntBuffer indexBuffer, int vertexCount) {

    public boolean canBeAdd(){
        return vertexCount > 0;
    }

}

