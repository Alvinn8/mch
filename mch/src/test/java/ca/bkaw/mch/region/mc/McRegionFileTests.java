package ca.bkaw.mch.region.mc;

import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class McRegionFileTests {
    static Path writeRegionFilePath = Path.of("run/test-run/r.0.0.mca");
    private final Path readRegionFilePath = Path.of("src/test/resources/region/r.0.0.mca");

    @BeforeAll
    static void setup() throws IOException {
        Files.createDirectories(writeRegionFilePath.getParent());
        Files.deleteIfExists(writeRegionFilePath);
    }

    private void readRegionFile(Path regionFilePath) throws Exception {
        try (McRegionFileReader regionFile = new McRegionFileReader(regionFilePath)) {
            DataInputStream stream = regionFile.readChunk(0, 0);
            NbtCompound chunkNbt = NbtTag.readCompound(stream);
            assertNotNull(chunkNbt.get("DataVersion"));
        }
    }

    @Test
    void readVanillaFile() throws Exception {
        this.readRegionFile(this.readRegionFilePath);
    }

    @Test
    void readZip() throws Exception {
        Path zipPath = Path.of("src/test/resources/region/r.0.0.mca.zip");
        try (FileSystem zip = FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()), new HashMap<>())) {
            this.readRegionFile(zip.getPath("r.0.0.mca"));
        }
    }

    @Test
    void writeFile() throws IOException {
        try (
            McRegionFileReader reader = new McRegionFileReader(this.readRegionFilePath);
            McRegionFileWriter writer = new McRegionFileWriter(writeRegionFilePath)
        ) {
            for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                for (int chunkX = 0; chunkX < 32; chunkX++) {
                    if (reader.hasChunk(chunkX, chunkZ)) {
                        NbtCompound chunkNbt = reader.readChunkNbt(chunkX, chunkZ);
                        writer.writeChunk(chunkNbt, reader.getChunkLastModified(chunkX, chunkZ));
                    }
                }
            }
        }
    }

    @Test
    void readWrittenFile() throws Exception {
        // Ensure we can parse the written region file again.
        // If that passes, the writer hopefully produces region files that
        // the game's parser also can read properly.
        this.readRegionFile(writeRegionFilePath);
    }

    @AfterAll
    static void cleanup() throws IOException {
        Files.deleteIfExists(writeRegionFilePath);
    }
}
