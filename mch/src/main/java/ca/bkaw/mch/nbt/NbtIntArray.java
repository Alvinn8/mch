package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class NbtIntArray implements NbtTag {

    public static final int ID = 11;

    private int[] value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        int size = dataInput.readInt();
        this.value = new int[size];
        for (int i = 0; i < size; i++) {
            this.value[i] = dataInput.readInt();
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(this.value.length);
        for (int integer : this.value) {
            dataOutput.writeInt(integer);
        }
    }

    public int[] getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "NbtIntArray{" + this.value.length + " ints}";
    }

    @Override
    public String createCompareReport(NbtTag tag) {
        NbtIntArray other = (NbtIntArray) tag;
        return Arrays.equals(other.value, this.value) ? "EQUAL" : "DIFF (" + this.value.length + " bytes, " + other.value.length + " bytes )";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != NbtIntArray.class) {
            return false;
        }
        return Arrays.equals(this.value, ((NbtIntArray) obj).value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.value);
    }

    @Override
    public int byteSize() {
        return 4 + this.value.length * 4;
    }
}
