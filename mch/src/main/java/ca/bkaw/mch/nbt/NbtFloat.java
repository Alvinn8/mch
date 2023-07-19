package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NbtFloat implements NbtTag {

    public static final int ID = 5;

    private float value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        this.value = dataInput.readFloat();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeFloat(this.value);
    }

    public float getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "NbtFloat{" + this.value + '}';
    }

    @Override
    public String createCompareReport(NbtTag tag, String indent) {
        NbtFloat other = (NbtFloat) tag;
        return other.value == this.value ? "EQUAL" : "DIFF (" + this.value + ", " + other.value + ')';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != NbtFloat.class) {
            return false;
        }
        return this.value == ((NbtFloat) obj).value;
    }

    @Override
    public int hashCode() {
        return (this.value != 0.0f ? Float.floatToIntBits(this.value) : 0);
    }

    @Override
    public int byteSize() {
        return 4;
    }
}
