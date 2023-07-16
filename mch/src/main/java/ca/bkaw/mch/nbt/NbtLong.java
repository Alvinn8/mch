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
}
