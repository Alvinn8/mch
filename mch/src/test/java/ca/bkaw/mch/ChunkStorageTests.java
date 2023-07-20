package ca.bkaw.mch;

import ca.bkaw.mch.chunk.ChunkStorage;
import ca.bkaw.mch.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChunkStorageTests {
    @Test
    public void test() throws IOException {
        NbtCompound chunkNbt = TestUtils.getChunkNbt("r.0.0.mca");
        System.out.println("chunkNbt = " + chunkNbt);

        ChunkStorage chunkStorage = new ChunkStorage();

        int versionNumber1 = chunkStorage.store(TestUtils.copyNbt(chunkNbt));
        assertEquals(1, versionNumber1);

        // Store the same chunk again. Expected behavior is that the chunk is
        // not saved anew and the old version number is returned.
        int versionNumber2 = chunkStorage.store(TestUtils.copyNbt(chunkNbt));
        assertEquals(1, versionNumber2);

        NbtCompound chunkNbt2 = TestUtils.getChunkNbt("r.0.0_v2.mca");
        int versionNumber3 = chunkStorage.store(TestUtils.copyNbt(chunkNbt2));
        assertEquals(2, versionNumber3);

        chunkStorage.test();

        // Serialize and deserialize
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        chunkStorage.write(new DataOutputStream(outBytes));
        byte[] bytes = outBytes.toByteArray();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(bytes);
        ChunkStorage readChunkStorage = new ChunkStorage(new DataInputStream(inBytes));

        int size1 = chunkNbt.byteSize();
        int size2 = chunkNbt2.byteSize();
        System.out.println(size1 + " + " + size2 + " = " + (size1 + size2));
        System.out.println("size1 = " + size1);
        System.out.println("outBytes.size() = " + outBytes.size());
        int compressedSize1 = compressedSize(chunkNbt);
        int compressedSize2 = compressedSize(chunkNbt2);
        System.out.println(compressedSize1 + " + " + compressedSize2 + " = " + (compressedSize1 + compressedSize2));
        System.out.println("mch: " + compressedSize(outBytes.toByteArray()));

        NbtCompound restoredChunkNbt1 = readChunkStorage.restore(versionNumber1);
        assertEquals(chunkNbt, restoredChunkNbt1);

        // NbtCompound restoredChunkNbt2 = readChunkStorage.restore(versionNumber3);
        // assertEquals(chunkNbt2, restoredChunkNbt2);
    }

    private int compressedSize(NbtCompound nbt) throws IOException {
        try (
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(new GZIPOutputStream(bytes))
        ) {
            nbt.write(stream);
            stream.close();
            return bytes.size();
        }
    }

    private int compressedSize(byte[] bytes) throws IOException {
        try (
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            GZIPOutputStream stream = new GZIPOutputStream(byteStream)
        ) {
            stream.write(bytes);
            stream.close();
            return byteStream.size();
        }
    }
}
