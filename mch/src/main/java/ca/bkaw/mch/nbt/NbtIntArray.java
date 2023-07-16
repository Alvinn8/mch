package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

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
}
