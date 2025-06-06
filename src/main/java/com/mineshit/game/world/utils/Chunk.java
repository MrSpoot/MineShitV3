package com.mineshit.game.world.utils;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Chunk {
    public static final int SIZE = 32;
    public static final int TOTAL_BLOCKS = SIZE * SIZE * SIZE;

    @Getter
    private final Vector3i position;

    @Getter @Setter
    private ChunkState state;

    private boolean isUniform = true;
    @Getter
    private short uniformBlockId = 0;

    private List<Short> palette;
    private int bitsPerBlock;
    private long[] data;

    public Chunk(Vector3i position) {
        this.position = new Vector3i(position);
        this.state = ChunkState.EMPTY;
    }

    public short getBlock(int x, int y, int z) {
        checkBounds(x, y, z);

        if (isUniform) {
            return uniformBlockId;
        }

        int index = getBlockIndex(x, y, z);
        int paletteIndex = readBlockData(index, data, bitsPerBlock);
        return palette.get(paletteIndex);
    }

    public short getBlockAtWorld(int worldX, int worldY, int worldZ) {
        int chunkWorldX = position.x * SIZE;
        int chunkWorldY = position.y * SIZE;
        int chunkWorldZ = position.z * SIZE;

        int localX = worldX - chunkWorldX;
        int localY = worldY - chunkWorldY;
        int localZ = worldZ - chunkWorldZ;

        if (isOutOfBounds(localX, localY, localZ)) {
            throw new IndexOutOfBoundsException("World coordinates not in this chunk: " + worldX + ", " + worldY + ", " + worldZ);
        }

        return getBlock(localX, localY, localZ);
    }


    public void setBlock(int x, int y, int z, BlockType block) {
        checkBounds(x, y, z);

        short blockId = block.getId();

        if (isUniform) {
            if (blockId == uniformBlockId) return;

            isUniform = false;
            initializePaletteAndData();
            fillUniformBlock();
        }

        int paletteIndex = palette.indexOf(blockId);
        if (paletteIndex == -1) {
            palette.add(blockId);
            paletteIndex = palette.size() - 1;
            ensureCapacity();
        }

        int index = getBlockIndex(x, y, z);
        writeBlockData(index, paletteIndex, data, bitsPerBlock);
    }

    public void setAllBlocks(short[] blocks) {
        if (blocks.length != TOTAL_BLOCKS) {
            throw new IllegalArgumentException("Block array must contain exactly " + TOTAL_BLOCKS + " elements.");
        }

        boolean uniform = true;
        short first = blocks[0];
        for (int i = 1; i < blocks.length; i++) {
            if (blocks[i] != first) {
                uniform = false;
                break;
            }
        }

        if (uniform) {
            this.fillChunk(Objects.requireNonNull(BlockType.fromId(first))); // ou juste: this.uniformBlockId = first; this.isUniform = true; ...
            return;
        }

        this.isUniform = false;
        this.initializePaletteAndData();

        for (int i = 0; i < blocks.length; i++) {
            short blockId = blocks[i];
            int paletteIndex = palette.indexOf(blockId);

            if (paletteIndex == -1) {
                palette.add(blockId);
                paletteIndex = palette.size() - 1;
                ensureCapacity();
            }

            writeBlockData(i, paletteIndex, data, bitsPerBlock);
        }
    }



    public void fillChunk(BlockType block) {
        short blockId = block.getId();
        isUniform = true;
        uniformBlockId = blockId;
        palette = null;
        data = null;
    }

    public boolean isUniform() {
        return isUniform;
    }

    public boolean isOutOfBounds(int x, int y, int z) {
        return (x < 0 || y < 0 || z < 0 || x >= SIZE || y >= SIZE || z >= SIZE);
    }

    public boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE &&
                y >= 0 && y < SIZE &&
                z >= 0 && z < SIZE;
    }

    public Vector3f getWorldMin() {
        return new Vector3f(
                position.x * SIZE,
                position.y * SIZE,
                position.z * SIZE
        );
    }

    public Vector3f getWorldMax() {
        return new Vector3f(
                (position.x + 1) * SIZE,
                (position.y + 1) * SIZE,
                (position.z + 1) * SIZE
        );
    }

    // --- Internal ---

    private void checkBounds(int x, int y, int z) {
        if (isOutOfBounds(x, y, z)) {
            throw new IndexOutOfBoundsException("Chunk coordinates out of bounds: " + x + ", " + y + ", " + z);
        }
    }

    private int getBlockIndex(int x, int y, int z) {
        return x + (z * SIZE) + (y * SIZE * SIZE);
    }

    private void initializePaletteAndData() {
        palette = new ArrayList<>();
        bitsPerBlock = 4;

        int totalBits = TOTAL_BLOCKS * bitsPerBlock;
        int dataLength = (totalBits + 63) / 64;
        data = new long[dataLength];

        if (!palette.contains((short) 0)) palette.add((short) 0);
        if (!palette.contains(uniformBlockId)) palette.add(uniformBlockId);
    }

    private void fillUniformBlock() {
        int paletteIndex = palette.indexOf(uniformBlockId);
        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            writeBlockData(i, paletteIndex, data, bitsPerBlock);
        }
    }

    private void ensureCapacity() {
        int requiredBits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
        if (requiredBits != bitsPerBlock) {
            reallocateData(requiredBits);
        }

    }

    private void reallocateData(int newBits) {
        int totalBits = TOTAL_BLOCKS * newBits;
        int dataLength = (totalBits + 63) / 64;
        long[] newData = new long[dataLength];

        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            int paletteIndex = readBlockData(i, data, bitsPerBlock);
            writeBlockData(i, paletteIndex, newData, newBits);
        }

        this.bitsPerBlock = newBits;
        this.data = newData;
    }


    private void writeBlockData(int index, int paletteIndex, long[] dataArray, int bitsPerBlock) {
        int bitIndex = index * bitsPerBlock;
        int arrayIndex = bitIndex / 64;
        int bitOffset = bitIndex % 64;

        long mask = ((1L << bitsPerBlock) - 1L) << bitOffset;
        dataArray[arrayIndex] = (dataArray[arrayIndex] & ~mask) | ((long) paletteIndex << bitOffset);

        if (64 - bitOffset < bitsPerBlock) {
            int remaining = bitsPerBlock - (64 - bitOffset);
            mask = (1L << remaining) - 1;
            dataArray[arrayIndex + 1] = (dataArray[arrayIndex + 1] & ~mask) | ((long) paletteIndex >> (bitsPerBlock - remaining));
        }
    }

    private int readBlockData(int index, long[] dataArray, int bitsPerBlock) {
        int bitIndex = index * bitsPerBlock;
        int arrayIndex = bitIndex / 64;
        int bitOffset = bitIndex % 64;

        long value = (dataArray[arrayIndex] >>> bitOffset) & ((1L << bitsPerBlock) - 1);

        if (64 - bitOffset < bitsPerBlock) {
            int remaining = bitsPerBlock - (64 - bitOffset);
            value |= (dataArray[arrayIndex + 1] & ((1L << remaining) - 1)) << (bitsPerBlock - remaining);
        }

        return (int) value;
    }
}
