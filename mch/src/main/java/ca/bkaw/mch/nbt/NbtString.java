package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class NbtString implements NbtTag {

    public static final int ID = 8;

    private String value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        this.value = dataInput.readUTF();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.value);
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "NbtString{" + this.value + '}';
    }

    @Override
    public String createCompareReport(NbtTag tag, String indent) {
        NbtString other = (NbtString) tag;
        return Objects.equals(other.value, this.value) ? "EQUAL" : "DIFF (" + this.value + ", " + other.value + ')';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != NbtString.class) {
            return false;
        }
        return Objects.equals(this.value, ((NbtString) obj).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public int byteSize() {
        return stringByteSize(this.value);
    }

    /**
     * Get the size in bytes to serialize the specified string using a {@link NbtString}
     * or using the {@link DataOutput#writeUTF(String)} method.
     *
     * @param str The string.
     * @return The amount of bytes.
     */
    public static int stringByteSize(String str) {
        return 2 + str.getBytes(StandardCharsets.UTF_8).length;
    }
}
