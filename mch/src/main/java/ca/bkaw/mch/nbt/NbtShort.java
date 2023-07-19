package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NbtShort implements NbtTag {

    public static final int ID = 2;

    private short value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        this.value = dataInput.readShort();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeShort(this.value);
    }

    public short getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "NbtShort{" + this.value + '}';
    }

    @Override
    public String createCompareReport(NbtTag tag, String indent) {
        NbtShort other = (NbtShort) tag;
        return other.value == this.value ? "EQUAL" : "DIFF (" + this.value + ", " + other.value + ')';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != NbtShort.class) {
            return false;
        }
        return this.value == ((NbtShort) obj).value;
    }

    @Override
    public int hashCode() {
        return this.value;
    }

    @Override
    public int byteSize() {
        return 2;
    }
}
