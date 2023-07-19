package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NbtLong implements NbtTag {

    public static final int ID = 4;

    private long value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        this.value = dataInput.readLong();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeLong(this.value);
    }

    public long getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "NbtLong{" + this.value + '}';
    }

    @Override
    public String createCompareReport(NbtTag tag, String indent) {
        NbtLong other = (NbtLong) tag;
        return other.value == this.value ? "EQUAL" : "DIFF (" + this.value + ", " + other.value + ')';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != NbtLong.class) {
            return false;
        }
        return this.value == ((NbtLong) obj).value;
    }

    @Override
    public int hashCode() {
        return (int) (this.value ^ (this.value >>> 32));
    }

    @Override
    public int byteSize() {
        return 8;
    }
}
