package ca.bkaw.mch.region;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.chunk.ChunkStorage;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtInt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * An inefficient object that opens the entire content of an mch region file into
 * memory.
 * <p>
 * This class can be used when chunks need to be read and modified with random
 * access within the mch region file, but in most cases the {@link MchRegionFileVisitor}
 * class should be preferred as it does not load the entire file into memory, which
 * decreases memory usage.
 */
public class MchRegionFile {
    public static final int MAGIC = FileMagic.REGION_STORAGE;
    public static final int CHUNK_COUNT = 32 * 32;

    private final Path path;
    private final Path parentFolder;
    private final int[] chunkLastModified = new int[CHUNK_COUNT];
    private final ChunkStorage[] chunkStorages = new ChunkStorage[CHUNK_COUNT];

    /**
     * Open an MchRegionFile for reading and writing.
     *
     * @param path The path to the mch region file.
     * @param parentFolder The parent folder of the path.
     * @throws IOException If an I/O error occurs.
     */
    public MchRegionFile(Path path, Path parentFolder) throws IOException {
        this.path = path;
        this.parentFolder = parentFolder;
        if (Files.exists(path)) {
            try (DataInputStream dataInput = new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
                if (dataInput.readInt() != MAGIC) {
                    throw new RuntimeException("Expected mch region file magic header. Is the mch region file corrupted?");
                }
                int mchVersion = dataInput.readInt();
                MchVersion.validate(mchVersion, 2);
                for (int i = 0; i < CHUNK_COUNT; i++) {
                    int chunkLastModified = dataInput.readInt();
                    this.chunkLastModified[i] = chunkLastModified;
                    ChunkStorage chunkStorage = new ChunkStorage(dataInput);
                    this.chunkStorages[i] = chunkStorage;
                }
            }
        }
    }

    public void write() throws IOException {
        Path tempFile = Files.createTempFile(this.parentFolder, this.parentFolder.getFileName().toString(), ".mchr-temp");
        try (DataOutputStream dataOutput = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(tempFile)))) {
            dataOutput.writeInt(MAGIC);
            dataOutput.writeInt(MchVersion.VERSION_NUMBER);
            for (int i = 0; i < CHUNK_COUNT; i++) {
                int chunkLastModified = this.chunkLastModified[i];
                dataOutput.writeInt(chunkLastModified);
                this.chunkStorages[i].write(dataOutput);
            }
        }
        Path oldFilePath = this.parentFolder.resolve(tempFile.getFileName() + "_old");
        if (Files.exists(this.path)) {
            Files.move(this.path, oldFilePath);
        }
        Files.move(tempFile, this.path);
        Files.deleteIfExists(oldFilePath);
    }

    static int getIndex(int chunkX, int chunkZ) {
        return (chunkX & 31) + (chunkZ & 31) * 32;
    }

    public int store(NbtCompound chunk) {
        NbtInt xPos = (NbtInt) chunk.get("xPos");
        NbtInt zPos = (NbtInt) chunk.get("zPos");
        if (xPos == null || zPos == null) {
            throw new IllegalArgumentException("The provided chunk nbt does not specify its coordinates.");
        }
        int index = getIndex(xPos.getValue(), zPos.getValue());

        ChunkStorage chunkStorage = this.chunkStorages[index];
        if (chunkStorage == null) {
            chunkStorage = new ChunkStorage();
            this.chunkStorages[index] = chunkStorage;
        }

        return chunkStorage.store(chunk);
    }
}
