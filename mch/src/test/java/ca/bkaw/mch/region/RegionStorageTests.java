package ca.bkaw.mch.region;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.region.mc.McRegionFileReader;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegionStorageTests {
    Path path = Path.of("run/test-run/r.0.0.mchrs.gz");

    @Test
    void test() throws IOException {
        Files.deleteIfExists(this.path);
        Files.createDirectories(this.path.getParent());

        saveRegionFile(this.path, "r.0.0.mca");
        saveRegionFile(this.path, "r.0.0_v2.mca");

        Sha1 sha1Before = Sha1.ofFile(this.path);
        RegionStorageVisitor.visit(this.path, chunk -> {
            // noop
        });
        Sha1 sha1After = Sha1.ofFile(this.path);

        // No changes were made, the file should be identical
        assertEquals(sha1Before, sha1After);
    }

    private void saveRegionFile(Path regionStoragePath, String regionFileName) throws IOException {
        Path regionFilePath = Path.of("src/test/resources/region/" + regionFileName);
        // Path regionFilePath = Path.of("../run/region/" + regionFileName);
        try (McRegionFileReader mcRegionFile = new McRegionFileReader(regionFilePath)) {
            RegionStorageVisitor.visit(regionStoragePath, chunk -> {
                if (mcRegionFile.hasChunk(chunk.getChunkX(), chunk.getChunkZ())) {
                    try (DataInputStream stream = mcRegionFile.readChunk(chunk.getChunkX(), chunk.getChunkZ())) {
                        NbtCompound chunkNbt = NbtTag.readCompound(stream);
                        chunk.store(chunkNbt);
                    }
                }
            });
        }
    }

    @Test
    void validateIndexLoopOrder() {
        int index = 0;
        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
            for (int chunkX = 0; chunkX < 32; chunkX++) {
                int computedIndex = McRegionFileReader.getIndex(chunkX, chunkZ);
                assertEquals(computedIndex, index);
                index++;
            }
        }
    }
}
