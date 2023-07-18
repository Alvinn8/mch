package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NbtDouble implements NbtTag {

    public static final int ID = 6;

    private double value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        this.value = dataInput.readDouble();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeDouble(this.value);
    }

    public double getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "NbtDouble{" + this.value + '}';
    }

    @Override
    public String createCompareReport(NbtTag tag) {
        NbtDouble other = (NbtDouble) tag;
        return other.value == this.value ? "EQUAL" : "DIFF (" + this.value + ", " + other.value + ')';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != NbtDouble.class) {
            return false;
        }
        return this.value == ((NbtDouble) obj).value;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(this.value);
        return (int) (temp ^ (temp >>> 32));
    }

    @Override
    public int byteSize() {
        return 8;
    }
}
