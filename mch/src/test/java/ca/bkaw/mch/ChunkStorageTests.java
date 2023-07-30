package ca.bkaw.mch;

import ca.bkaw.mch.chunk.ChunkStorage;
import ca.bkaw.mch.chunk.RegionFileChunk;
import ca.bkaw.mch.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChunkStorageTests {
    @Test
    public void test() throws IOException {
        NbtCompound chunkNbt = TestUtils.getChunkNbt("r.0.0.mca");

        ChunkStorage chunkStorage = new ChunkStorage();

        int versionNumber1 = chunkStorage.store(TestUtils.copyNbt(chunkNbt), 123);
        assertEquals(1, versionNumber1);

        // Store the same chunk again. Expected behavior is that the chunk is
        // not saved anew and the old version number is returned.
        int versionNumber2 = chunkStorage.store(TestUtils.copyNbt(chunkNbt), 123);
        assertEquals(1, versionNumber2);

        NbtCompound chunkNbt2 = TestUtils.getChunkNbt("r.0.0_v2.mca");
        int versionNumber3 = chunkStorage.store(TestUtils.copyNbt(chunkNbt2), 123);
        assertEquals(2, versionNumber3);

        // Serialize and deserialize
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        chunkStorage.write(new DataOutputStream(outBytes));
        byte[] bytes = outBytes.toByteArray();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(bytes);
        ChunkStorage readChunkStorage = new ChunkStorage(new DataInputStream(inBytes));

        RegionFileChunk restoredChunk1 = readChunkStorage.restore(versionNumber1);
        assertEquals(chunkNbt, restoredChunk1.nbt());
        assertEquals(123, restoredChunk1.lastModified());

        RegionFileChunk restoredChunk2 = readChunkStorage.restore(versionNumber3);
        assertEquals(chunkNbt2, restoredChunk2.nbt());
        assertEquals(123, restoredChunk2.lastModified());
    }
}
