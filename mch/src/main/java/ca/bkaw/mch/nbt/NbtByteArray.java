package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NbtByteArray implements NbtTag {

    public static final int ID = 7;

    private byte[] value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        int size = dataInput.readInt();
        this.value = new byte[size];
        dataInput.readFully(this.value);
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(this.value.length);
        dataOutput.write(this.value);
    }
}
