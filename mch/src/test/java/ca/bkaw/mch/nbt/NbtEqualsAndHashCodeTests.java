package ca.bkaw.mch.nbt;

import ca.bkaw.mch.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NbtEqualsAndHashCodeTests {
    static NbtCompound nbt;

    @BeforeAll
    static void readNbt() throws IOException {
        nbt = TestUtils.readUncompressed1();
    }

    @Test
    void byteTest() throws IOException {
        validateEqualsAndHashCode("byte");
    }

    @Test
    void intTest() throws IOException {
        validateEqualsAndHashCode("int");
    }

    @Test
    void shortTest() throws IOException {
        validateEqualsAndHashCode("short");
    }

    @Test
    void floatTest() throws IOException {
        validateEqualsAndHashCode("float");
    }

    @Test
    void doubleTest() throws IOException {
        validateEqualsAndHashCode("double");
    }

    @Test
    void stringTest() throws IOException {
        validateEqualsAndHashCode("string");
    }

    @Test
    void longTest() throws IOException {
        validateEqualsAndHashCode("long");
    }

    @Test
    void byteArrayTest() throws IOException {
        validateEqualsAndHashCode("byte_array");
    }

    @Test
    void intArrayTest() throws IOException {
        validateEqualsAndHashCode("int_array");
    }

    @Test
    void longArrayTest() throws IOException {
        validateEqualsAndHashCode("long_array");
    }

    @Test
    void listTest() throws IOException {
        validateEqualsAndHashCode("list");
    }

    @Test
    void compoundTest() throws IOException {
        validateEqualsAndHashCode(nbt);
    }

    private void validateEqualsAndHashCode(String key) throws IOException {
        NbtTag tag = nbt.get(key);
        assertNotNull(tag);
        validateEqualsAndHashCode(tag);
    }

    private void validateEqualsAndHashCode(NbtTag tag) throws IOException {
        NbtTag copy = TestUtils.copyNbt(tag);
        assertEquals(tag, copy);
        assertEquals(tag.hashCode(), copy.hashCode(), tag.getClass().getSimpleName() + " hashCode");
    }

}
