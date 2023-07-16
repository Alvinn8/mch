package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NbtInt implements NbtTag {

    public static final int ID = 3;

    private int value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        this.value = dataInput.readInt();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(this.value);
    }

    public int getValue() {
        return this.value;
    }
}
