package ca.bkaw.mch.region;

import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.region.mc.McRegionFile;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MchRegionFileTests {
    @Test
    void test() throws IOException {
        Path path = Path.of("run/test-run/r.0.0.mchr");
        Files.deleteIfExists(path);
        Files.createDirectories(path.getParent());

        MchRegionFile mchRegionFile = new MchRegionFile(path, path.getParent());

        saveRegionFile(mchRegionFile, "r.0.0.mca");
        saveRegionFile(mchRegionFile, "r.0.0_v2.mca");

        mchRegionFile.write();
    }

    private void saveRegionFile(MchRegionFile mchRegionFile, String regionFileName) throws IOException {
        // Path regionFilePath = Path.of("src/test/resources/region/" + regionFileName);
        Path regionFilePath = Path.of("../run/region/" + regionFileName);
        try (McRegionFile mcRegionFile = new McRegionFile(regionFilePath)) {
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    if (mcRegionFile.hasChunk(x, z)) {
                        try (DataInputStream stream = mcRegionFile.readChunk(x, z)) {
                            NbtCompound chunkNbt = NbtTag.readCompound(stream);
                            mchRegionFile.writeNewChunk(chunkNbt);
                            System.out.println("Writing chunk " + x + " " + z + " (" + chunkNbt.byteSize() + ")");
                        }
                    }
                }
            }
        }
    }
}
