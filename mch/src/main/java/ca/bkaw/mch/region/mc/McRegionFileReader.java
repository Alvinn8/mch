package ca.bkaw.mch.region.mc;

import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.util.RandomAccessReader;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.InflaterInputStream;

/**
 * An object that once created opens a region file for reading chunks.
 */
public class McRegionFileReader implements AutoCloseable {
    public static final int CHUNK_COUNT = 32 * 32;
    public static final int SECTOR_SIZE = 4096;

    private final RandomAccessReader file;
    private final int[] locations = new int[CHUNK_COUNT];
    private final int[] lastModified = new int[CHUNK_COUNT];

    public McRegionFileReader(Path path) throws IOException {
        this(RandomAccessReader.of(path));
    }

    public McRegionFileReader(RandomAccessReader file) throws IOException {
        this.file = file;
        for (int i = 0; i < CHUNK_COUNT; i++) {
            this.locations[i] = this.file.readInt();
        }
        for (int i = 0; i < CHUNK_COUNT; i++) {
            this.lastModified[i] = this.file.readInt();
        }
    }

    public static int getIndex(int chunkX, int chunkZ) {
        return (chunkX & 31) + (chunkZ & 31) * 32;
    }

    public boolean hasChunk(int chunkX, int chunkZ) {
        return this.locations[getIndex(chunkX, chunkZ)] != 0;
    }

    /**
     * Read a data input stream from chunk coordinates.
     *
     * @param chunkX The chunk x coordinate.
     * @param chunkZ The chunk z coordinate.
     * @return The data input stream.
     * @throws IOException If an I/O error occurs.
     */
    public DataInputStream readChunk(int chunkX, int chunkZ) throws IOException {
        int index = getIndex(chunkX, chunkZ);

        int location = this.locations[index];
        if (location == 0) {
            throw new RuntimeException("The chunk " + chunkX + " " + chunkZ + " is empty in this region file.");
        }
        // The location holds has two different values
        // 0 1 2   3
        // offset  sector count
        int offsetSector = location >> 8 & 0x00ffffff; // first 3 bytes
        // location & 0x000000ff; // 4th byte is the sectorLength.
        // The sectorLength is not used here because we read the precise length directly.

        int byteOffset = offsetSector * SECTOR_SIZE;
        this.file.seek(byteOffset);
        int length = this.file.readInt();
        byte compressionType = this.file.readByte();
        if (compressionType != 2) {
            throw new UnsupportedOperationException("Only compression type 2 (Zlib) is supported.");
        }
        byte[] bytes = new byte[length - 1];
        this.file.readFully(bytes);

        return new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes)));
    }

    /**
     * Read chunk nbt.
     *
     * @param chunkX The chunk x coordinate.
     * @param chunkZ The chunk z coordinate.
     * @return The nbt compound.
     * @throws IOException If an I/O error occurs.
     */
    public NbtCompound readChunkNbt(int chunkX, int chunkZ) throws IOException {
        try (DataInputStream stream = this.readChunk(chunkX, chunkZ)) {
            return NbtTag.readCompound(stream);
        }
    }

    /**
     * Get the time the chunk was marked as last modified, in epoch seconds.
     * <p>
     * The last modified time is stored in the region file header and can be read
     * without having to read the chunk.
     *
     * @param chunkX The chunk x coordinate.
     * @param chunkZ The chunk x coordinate.
     * @return The last modified time, in epoch seconds.
     */
    public int getChunkLastModified(int chunkX, int chunkZ) {
        int lastModified = this.lastModified[getIndex(chunkX, chunkZ)];
        if (lastModified == 0) {
            throw new RuntimeException("The chunk " + chunkX + " " + chunkZ + " is empty in this region file.");
        }
        return lastModified;
    }

    @Override
    public void close() throws IOException {
        this.file.close();
    }
}
