package ca.bkaw.mch;

import ca.bkaw.mch.chunk.MchChunkStorage;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.nbt.NbtTests;
import ca.bkaw.mch.region.McRegionFile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MchChunkStorageTests {
    @Test
    public void test() throws IOException {
        System.out.println("start test");
        NbtCompound chunkNbt = this.getChunkNbt("r.0.0.mca");

        MchChunkStorage chunkStorage = new MchChunkStorage();

        int versionNumber1 = chunkStorage.store(NbtTests.copyNbt(chunkNbt));
        assertEquals(1, versionNumber1);

        // Store the same chunk again. Expected behavior is that the chunk is
        // not saved anew and the old version number is returned.
        int versionNumber2 = chunkStorage.store(NbtTests.copyNbt(chunkNbt));
        assertEquals(1, versionNumber2);

        NbtCompound chunkNbt2 = this.getChunkNbt("r.0.0_v2.mca");
        int versionNumber3 = chunkStorage.store(NbtTests.copyNbt(chunkNbt2));
        assertEquals(2, versionNumber3);

        chunkStorage.test();

        // Serialize and deserialize
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        chunkStorage.write(new DataOutputStream(outBytes));
        ByteArrayInputStream inBytes = new ByteArrayInputStream(outBytes.toByteArray());
        MchChunkStorage readChunkStorage = new MchChunkStorage(new DataInputStream(inBytes));

        int size1 = chunkNbt.byteSize();
        int size2 = chunkNbt2.byteSize();
        System.out.println(size1 + " + " + size2 + " = " + (size1 + size2));
        System.out.println("outBytes.size() = " + outBytes.size());

        NbtCompound restoredChunkNbt1 = readChunkStorage.restore(versionNumber1);
        assertEquals(chunkNbt, restoredChunkNbt1);

        NbtCompound restoredChunkNbt2 = readChunkStorage.restore(versionNumber3);
        assertEquals(chunkNbt, restoredChunkNbt2);
        System.out.println("end test");
    }

    private NbtCompound getChunkNbt(String regionFileName) throws IOException {
        Path regionFilePath = Path.of("src/test/resources/region/" + regionFileName);
        try (McRegionFile regionFile = new McRegionFile(regionFilePath)) {
            DataInputStream stream = regionFile.readChunk(0, 0);
            return NbtTag.readCompound(stream);
        }
    }
}
