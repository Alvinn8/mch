package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class NbtByteArray implements NbtTag {

    public static final int ID = 7;

    private byte[] value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        int size = dataInput.readInt();
        this.value = new byte[size];
        dataInput.readFully(this.value);
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(this.value.length);
        dataOutput.write(this.value);
    }

    public byte[] getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "NbtByteArray{" + this.value.length + " bytes}";
    }

    @Override
    public String createCompareReport(NbtTag tag, String indent) {
        NbtByteArray other = (NbtByteArray) tag;
        return Arrays.equals(other.value, this.value) ? "EQUAL" : "DIFF (" + this.value.length + " bytes, " + other.value.length + " bytes )";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != NbtByteArray.class) {
            return false;
        }
        return Arrays.equals(this.value, ((NbtByteArray) obj).value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.value);
    }

    @Override
    public int byteSize() {
        return 4 + this.value.length;
    }
}
