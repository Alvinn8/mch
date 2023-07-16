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
}
