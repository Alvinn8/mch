package ca.bkaw.mch.region;

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

// "Make it work, then make it fast"

public class MchRegionFile {
    public static final int MAGIC = 0x6D6368_72;
    public static final int CHUNK_COUNT = 32 * 32;

    private final Path path;
    private final Path parentFolder;
    private final int[] chunkVersionNumbers = new int[CHUNK_COUNT];
    private final ChunkStorage[] chunkStorages = new ChunkStorage[CHUNK_COUNT];

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
                    int chunkVersionNumber = dataInput.readInt();
                    ChunkStorage chunkStorage = new ChunkStorage(dataInput);
                    this.chunkVersionNumbers[i] = chunkVersionNumber;
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
                int chunkVersionNumber = this.chunkVersionNumbers[i];
                dataOutput.writeInt(chunkVersionNumber);
                if (chunkVersionNumber != 0) {
                    this.chunkStorages[i].write(dataOutput);
                }
            }
        }
        Path oldFilePath = this.parentFolder.resolve(tempFile.getFileName() + "_old");
        if (Files.exists(this.path)) {
            Files.move(this.path, oldFilePath);
        }
        Files.move(tempFile, this.path);
        Files.deleteIfExists(oldFilePath);
    }

    private static int getIndex(int chunkX, int chunkZ) {
        return (chunkX & 31) + (chunkZ & 31) * 32;
    }

    public void writeNewChunk(NbtCompound chunk) {
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

        int chunkVersionNumber = chunkStorage.store(chunk);
        this.chunkVersionNumbers[index] = chunkVersionNumber;
    }
}
