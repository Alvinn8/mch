package ca.bkaw.mch.nbt;

import ca.bkaw.mch.TestUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NbtTests {

    @Test
    public void readValues() throws IOException {
        NbtCompound nbt = TestUtils.readUncompressed1();

        // Assert reading
        assertEquals((byte) 10, get(nbt, "byte", NbtByte.class).getValue());
        assertEquals((short) 3328, get(nbt, "short", NbtShort.class).getValue());
        assertEquals(100665344, get(nbt, "int", NbtInt.class).getValue());
        assertEquals(576475546090694771L, get(nbt, "long", NbtLong.class).getValue());
        assertEquals(1.5048164E-36F, get(nbt, "float", NbtFloat.class).getValue());
        assertEquals(3.791732915075566e-270, get(nbt, "double", NbtDouble.class).getValue());
        assertEquals("Hello, World!", get(nbt, "string", NbtString.class).getValue());
        assertArrayEquals(new byte[]{33, 115}, get(nbt, "byte_array", NbtByteArray.class).getValue());
        assertArrayEquals(new int[]{3}, get(nbt, "int_array", NbtIntArray.class).getValue());
        assertArrayEquals(new long[]{3}, get(nbt, "long_array", NbtLongArray.class).getValue());
        NbtList nbtList = get(nbt, "list", NbtList.class);
        assertEquals(NbtString.ID, nbtList.getListTypeId());
        NbtTag firstElement = nbtList.getValue()[0];
        assertEquals(firstElement.getClass(), NbtString.class);
        assertEquals("test1", ((NbtString) firstElement).getValue());
    }

    private <T extends NbtTag> T get(NbtCompound nbtCompound, String key, Class<T> clazz) {
        NbtTag tag = nbtCompound.get(key);
        assertNotNull(tag, "Tag " + key + " did not exist in compound.");
        assertInstanceOf(clazz, tag);
        return clazz.cast(tag);
    }

    @Test
    public void readWriteSameResult() throws IOException {
        NbtCompound nbt = TestUtils.readUncompressed1();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(baos);
        stream.writeByte(NbtCompound.ID);
        stream.writeUTF("root");
        nbt.write(stream);

        InputStream inputStream = NbtTests.class.getClassLoader().getResourceAsStream("nbt/uncompressed1.nbt");
        assertNotNull(inputStream);
        byte[] expectedBytes = inputStream.readAllBytes();

        byte[] foundBytes = baos.toByteArray();

        assertArrayEquals(expectedBytes, foundBytes);
    }

    @Test
    public void byteSizes() throws IOException {
        NbtCompound nbt = TestUtils.readUncompressed1();

        for (Map.Entry<String, NbtTag> entry : nbt.entrySet()) {
            NbtTag tag = entry.getValue();
            this.validateByteSize(tag);
        }

        this.validateByteSize(nbt);
    }

    private void validateByteSize(NbtTag nbt) throws IOException {
        try (
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bytes)
        ) {
            nbt.write(stream);

            assertEquals(bytes.toByteArray().length, nbt.byteSize());
        }
    }
}
