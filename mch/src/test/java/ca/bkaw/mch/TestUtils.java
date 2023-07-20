package ca.bkaw.mch;

import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.nbt.NbtTests;
import ca.bkaw.mch.region.mc.McRegionFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestUtils {
    public static NbtCompound getChunkNbt(String regionFileName) throws IOException {
        Path regionFilePath = Path.of("src/test/resources/region/" + regionFileName);
        try (McRegionFile regionFile = new McRegionFile(regionFilePath)) {
            DataInputStream stream = regionFile.readChunk(0, 0);
            return NbtTag.readCompound(stream);
        }
    }

    public static NbtCompound readUncompressed1() throws IOException {
        InputStream inputStream = NbtTests.class.getClassLoader().getResourceAsStream("nbt/uncompressed1.nbt");
        assertNotNull(inputStream);
        DataInputStream stream = new DataInputStream(inputStream);

        assertEquals(stream.readByte(), NbtCompound.ID);
        stream.readUTF();
        NbtCompound nbt = new NbtCompound();
        nbt.read(stream);

        return nbt;
    }

    @SuppressWarnings("unchecked")
    public static <T extends NbtTag> T copyNbt(T nbt) throws IOException {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(outBytes);
        NbtTag.writeTag(dataOutput, nbt);

        ByteArrayInputStream inBytes = new ByteArrayInputStream(outBytes.toByteArray());
        DataInputStream dataInput = new DataInputStream(inBytes);
        return (T) NbtTag.readTag(dataInput);
    }
}
