package ca.bkaw.mch.region.mc;

import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtInt;
import ca.bkaw.mch.nbt.NbtTag;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;

/**
 * An object that creates a new region file for writing chunks.
 */
public class McRegionFileWriter implements AutoCloseable {
    private final static int CHUNK_COUNT = McRegionFileReader.CHUNK_COUNT;
    private final static int SECTOR_SIZE = McRegionFileReader.SECTOR_SIZE;

    private final RandomAccessFile file;
    private final int[] locations = new int[CHUNK_COUNT];
    private final int[] lastModified = new int[CHUNK_COUNT];

    public McRegionFileWriter(Path path) throws IOException {
        if (Files.exists(path)) {
            throw new UnsupportedOperationException(
                "A McRegionFileWriter can not edit existing region files."
            );
        }
        this.file = new RandomAccessFile(path.toFile(), "rw");
        this.file.seek(2 * SECTOR_SIZE);
    }

    @Override
    public void close() throws IOException {
        try {
            // Write header
            this.file.seek(0);
            for (int location : this.locations) {
                this.file.writeInt(location);
            }
            for (int value : this.lastModified) {
                this.file.writeInt(value);
            }
        } finally {
            this.file.close();
        }
    }

    private long getNextSector(long location) {
        return (long) Math.ceil((double) location / SECTOR_SIZE);
    }

    public void writeChunk(NbtCompound chunkNbt, int lastModified) throws IOException {
        NbtInt xPos = (NbtInt) chunkNbt.get("xPos");
        NbtInt zPos = (NbtInt) chunkNbt.get("zPos");
        if (xPos == null || zPos == null) {
            // Some older versions of the game store all data in a nested "Level" tag. While
            // mch mainly targets newer versions of the game, there are still cases where a
            // world needs to be restored that has some chunks that have not been touched in
            // a long time and therefore have chunks in this old format, despite the world
            // being loaded and saved in the latest version.
            if (chunkNbt.get("Level") instanceof NbtCompound level) {
                xPos = (NbtInt) level.get("xPos");
                zPos = (NbtInt) level.get("zPos");
            }
        }
        if (xPos == null || zPos == null) {
            throw new IllegalArgumentException("The provided chunk nbt does not specify its coordinates.");
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(bytes)));
        NbtTag.writeTag(stream, chunkNbt);
        stream.close();
        byte[] chunkBytes = bytes.toByteArray();

        this.writeChunk(chunkBytes, xPos.getValue(), zPos.getValue(), lastModified);
    }

    private void writeChunk(byte[] chunkBytes, int chunkX, int chunkZ, int lastModified) throws IOException {
        long offset = this.file.getFilePointer();
        if (offset % SECTOR_SIZE != 0) {
            throw new RuntimeException("File pointer was not at a sector.");
        }
        int offsetSector = (int) (offset / SECTOR_SIZE);
        if (offsetSector < 2) {
            throw new RuntimeException("Invalid sector for chunk. File pointer was not offset correctly.");
        }
        this.file.writeInt(chunkBytes.length + 1);
        this.file.writeByte(2);
        this.file.write(chunkBytes);

        long endOffset = this.file.getFilePointer();
        if (endOffset % SECTOR_SIZE != 0) {
            // Pad to next sector
            long nextSector = this.getNextSector(endOffset);
            this.file.seek(nextSector * SECTOR_SIZE - 1);
            this.file.writeByte(0);
            // Seek will not change the length of the file until we write. We therefore
            // need to write one byte at the last byte so that the rest are filled too.
        }

        byte sectorSize = (byte) Math.ceil((double) (endOffset - offset) / SECTOR_SIZE);

        int index = McRegionFileReader.getIndex(chunkX, chunkZ);
        int location = offsetSector << 8 | sectorSize;
        this.locations[index] = location;
        this.lastModified[index] = lastModified;
    }
}
