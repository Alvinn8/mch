package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NbtByte implements NbtTag {

    public static final int ID = 1;

    private byte value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        this.value = dataInput.readByte();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeByte(this.value);
    }

    public byte getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "NbtByte{" + this.value + '}';
    }

    @Override
    public String createCompareReport(NbtTag tag) {
        NbtByte other = (NbtByte) tag;
        return other.value == this.value ? "EQUAL" : "DIFF (" + this.value + ", " + other.value + ')';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != NbtByte.class) {
            return false;
        }
        return this.value == ((NbtByte) obj).value;
    }
}
