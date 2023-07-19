package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class NbtLongArray implements NbtTag {

    public static final int ID = 12;

    private long[] value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        int size = dataInput.readInt();
        this.value = new long[size];
        for (int i = 0; i < size; i++) {
            this.value[i] = dataInput.readLong();
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(this.value.length);
        for (long l : this.value) {
            dataOutput.writeLong(l);
        }
    }

    public long[] getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "NbtLongArray{" + this.value.length + " longs}";
    }

    @Override
    public String createCompareReport(NbtTag tag, String indent) {
        NbtLongArray other = (NbtLongArray) tag;
        return Arrays.equals(other.value, this.value) ? "EQUAL" : "DIFF (" + this.value.length + " bytes, " + other.value.length + " bytes )";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != NbtLongArray.class) {
            return false;
        }
        return Arrays.equals(this.value, ((NbtLongArray) obj).value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.value);
    }

    @Override
    public int byteSize() {
        return 4 + this.value.length * 8;
    }
}
