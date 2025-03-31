package com.mineshit.game;

import com.mineshit.game.world.Chunk;
import org.joml.Vector3i;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ChunkTest {

    private Chunk chunk;

    @BeforeEach
    public void setup() {
        chunk = new Chunk(new Vector3i(0, 0, 0));
    }

    @Test
    public void testUniformBlockInitialization() {
        assertTrue(chunk.isUniform());
        assertEquals(0, chunk.getUniformBlockId());
    }

    @Test
    public void testSetAndGetBlockWithinBounds() {
        chunk.setBlock(1, 1, 1, (short) 5);
        assertFalse(chunk.isUniform());
        assertEquals(5, chunk.getBlock(1, 1, 1));
    }

    @Test
    public void testGetBlockOutsideBoundsThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(-1, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(0, -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(0, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(Chunk.SIZE, 0, 0));
    }

    @Test
    public void testSetSameBlockInUniformKeepsUniform() {
        chunk.setBlock(0, 0, 0, (short) 0);
        assertTrue(chunk.isUniform());
        assertEquals(0, chunk.getBlock(0, 0, 0));
    }

    @Test
    public void testFillChunk() {
        chunk.fillChunk((short) 7);
        assertTrue(chunk.isUniform());
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    assertEquals(7, chunk.getBlock(x, y, z));
                }
            }
        }
    }

    @Test
    public void testPaletteCompressionExpansion() {
        for (int i = 0; i < 20; i++) {
            chunk.setBlock(i % Chunk.SIZE, (i / Chunk.SIZE) % Chunk.SIZE, 0, (short) i);
        }
        // Just check that data was set without errors and not uniform
        assertFalse(chunk.isUniform());
        assertTrue(chunk.getBlock(0, 0, 0) >= 0);
    }
}

