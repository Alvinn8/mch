package ca.bkaw.mch.region;

import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.region.mc.McRegionFile;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RegionFileTests {
    private void readRegionFile(Path regionFilePath) throws Exception {
        try (McRegionFile regionFile = new McRegionFile(regionFilePath)) {
            DataInputStream stream = regionFile.readChunk(0, 0);
            NbtCompound chunkNbt = NbtTag.readCompound(stream);
            assertNotNull(chunkNbt.get("DataVersion"));
        }
    }

    @Test
    public void readFile() throws Exception {
        this.readRegionFile(Path.of("src/test/resources/region/r.0.0.mca"));
    }

    @Test
    public void readZip() throws Exception {
        Path zipPath = Path.of("src/test/resources/region/r.0.0.mca.zip");
        try (FileSystem zip = FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()), new HashMap<>())) {
            this.readRegionFile(zip.getPath("r.0.0.mca"));
        }
    }
}
