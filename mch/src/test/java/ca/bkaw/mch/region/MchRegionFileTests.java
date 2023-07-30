package ca.bkaw.mch.region;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MchRegionFileTests {
    @Test
    void test() throws IOException {
        Path path = Path.of("run/r.0.0.mchrv.gz");
        Files.deleteIfExists(path);
        Files.createDirectories(path.getParent());

        int[] chunkVersionNumbers1 = new int[1024];
        Arrays.fill(chunkVersionNumbers1, 1);

        int regionFileVersionNumber1 = MchRegionFile.store(path, chunkVersionNumbers1);
        assertEquals(1, regionFileVersionNumber1);

        int[] chunkVersionNumbers2 = new int[1024];
        Arrays.fill(chunkVersionNumbers2, 2);

        int regionFileVersionNumber2 = MchRegionFile.store(path, chunkVersionNumbers2);
        assertEquals(2, regionFileVersionNumber2);

        // Save the first array again and expect to get the existing version number
        int regionFileVersionNumber3 = MchRegionFile.store(path, chunkVersionNumbers1);
        assertEquals(1, regionFileVersionNumber3);

        int[] chunkVersionNumbers3 = MchRegionFile.read(path, 2);
        assertArrayEquals(chunkVersionNumbers2, chunkVersionNumbers3);

        Files.delete(path);
    }
}
