package ca.bkaw.mch.region;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.region.mc.McRegionFile;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MchRegionFileTests {
    Path path = Path.of("run/test-run/r.0.0.mchr");

    @Test
    void test() throws IOException {
        Files.deleteIfExists(this.path);
        Files.createDirectories(this.path.getParent());

        MchRegionFile mchRegionFile = new MchRegionFile(this.path, this.path.getParent());

        saveRegionFile(mchRegionFile, "r.0.0.mca");
        saveRegionFile(mchRegionFile, "r.0.0_v2.mca");

        mchRegionFile.write();
    }

    private void saveRegionFile(MchRegionFile mchRegionFile, String regionFileName) throws IOException {
        Path regionFilePath = Path.of("src/test/resources/region/" + regionFileName);
        // Path regionFilePath = Path.of("../run/region/" + regionFileName);
        try (McRegionFile mcRegionFile = new McRegionFile(regionFilePath)) {
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    if (mcRegionFile.hasChunk(x, z)) {
                        try (DataInputStream stream = mcRegionFile.readChunk(x, z)) {
                            NbtCompound chunkNbt = NbtTag.readCompound(stream);
                            mchRegionFile.store(chunkNbt);
                        }
                    }
                }
            }
        }
    }

    @Test
    void validateIndexLoopOrder() {
        int index = 0;
        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
            for (int chunkX = 0; chunkX < 32; chunkX++) {
                int computedIndex = MchRegionFile.getIndex(chunkX, chunkZ);
                assertEquals(computedIndex, index);
                index++;
            }
        }
    }

    @Test
    void test2() throws IOException {
        Sha1 sha1Before = Sha1.ofFile(this.path);
        MchRegionFileVisitor.visit(this.path, chunk -> {
            // noop
        });
        Sha1 sha1After = Sha1.ofFile(this.path);

        // No changes were made, the file should be identical
        assertEquals(sha1Before, sha1After);
    }
}
