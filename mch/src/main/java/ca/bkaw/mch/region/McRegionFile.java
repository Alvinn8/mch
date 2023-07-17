package ca.bkaw.mch.region;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.InflaterInputStream;

public class McRegionFile implements AutoCloseable {
    public static final int CHUNK_COUNT = 32 * 32;
    public static final int SECTOR_SIZE = 4096;

    private final RandomAccessFile file;
    private final int[] locations = new int[CHUNK_COUNT];
    private final int[] lastModified = new int[CHUNK_COUNT];

    public McRegionFile(File file) throws IOException {
        this.file = new RandomAccessFile(file, "r");
        for (int i = 0; i < CHUNK_COUNT; i++) {
            this.locations[i] = this.file.readInt();
        }
        for (int i = 0; i < CHUNK_COUNT; i++) {
            this.lastModified[i] = this.file.readInt();
        }
    }

    private static int getIndex(int chunkX, int chunkZ) {
        return (chunkX & 31) + (chunkZ & 31) * 32;
    }

    public DataInputStream readChunk(int chunkX, int chunkZ) throws IOException {
        int index = getIndex(chunkX, chunkZ);

        // The location holds has two different values
        // 0 1 2   3
        // offset  sector count
        int location = this.locations[index];
        int offsetSector = location >> 8 & 0x00ffffff; // first 3 bytes
        int sectorLength = location & 0x000000ff; // 4th byte

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

    @Override
    public void close() throws Exception {
        this.file.close();
    }
}
