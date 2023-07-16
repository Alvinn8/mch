package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NbtLongArray implements NbtTag {

    public static final int ID = 12;

    private long[] value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        int size = dataInput.readInt();
        this.value = new long[size];
        for (int i = 0; i < size; i++) {
            this.value[i] = dataInput.readLong();
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(this.value.length);
        for (long l : this.value) {
            dataOutput.writeLong(l);
        }
    }

    public long[] getValue() {
        return this.value;
    }
}
