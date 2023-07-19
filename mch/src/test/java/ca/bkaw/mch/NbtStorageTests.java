package ca.bkaw.mch;

import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtString;
import ca.bkaw.mch.nbt.NbtTag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NbtStorageTests {
    @Test
    public void test() throws IOException {
        byte[] bytes = this.write();
        this.read(bytes);
    }

    private byte[] write() throws IOException {
        NbtStorage storage = new NbtStorage();

        NbtCompound nbt1 = new NbtCompound();
        NbtString str1 = new NbtString();
        str1.setValue("test1");
        nbt1.set("test", str1);
        int versionNumber1 = storage.store(nbt1);
        assertEquals(1, versionNumber1);

        NbtCompound nbt2 = new NbtCompound();
        NbtString str2 = new NbtString();
        str2.setValue("test2");
        nbt2.set("test", str2);
        int versionNumber2 = storage.store(nbt2);
        assertEquals(2, versionNumber2);

        NbtCompound nbt1Copy = this.copyNbt(nbt1);
        int versionNumber3 = storage.store(nbt1Copy);
        assertEquals(1, versionNumber3, "An existing nbt was not reused.");

        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(outBytes);

        storage.write(output);

        return outBytes.toByteArray();
    }

    private void read(byte[] bytes) throws IOException {
        ByteArrayInputStream inBytes = new ByteArrayInputStream(bytes);
        DataInputStream input = new DataInputStream(inBytes);

        NbtStorage storage = new NbtStorage(input);

        NbtCompound nbt1 = storage.get(1);
        NbtString str1 = (NbtString) nbt1.get("test");
        assertNotNull(str1);
        assertEquals("test1", str1.getValue());

        NbtCompound nbt2 = storage.get(2);
        NbtString str2 = (NbtString) nbt2.get("test");
        assertNotNull(str2);
        assertEquals("test2", str2.getValue());
    }

    private NbtCompound copyNbt(NbtCompound nbt) throws IOException {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(outBytes);
        NbtTag.writeTag(dataOutput, nbt);

        ByteArrayInputStream inBytes = new ByteArrayInputStream(outBytes.toByteArray());
        DataInputStream dataInput = new DataInputStream(inBytes);
        return NbtTag.readCompound(dataInput);
    }
}
