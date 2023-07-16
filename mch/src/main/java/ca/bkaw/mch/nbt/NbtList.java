package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NbtList implements NbtTag {

    public static final int ID = 9;

    private byte listTypeId;
    private NbtTag[] value;

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        this.listTypeId = dataInput.readByte();

        int size = dataInput.readInt();
        this.value = new NbtTag[size];

        // End tags are only allowed as a type if the list is empty.
        if (this.listTypeId == NbtEnd.ID && size > 0) {
            throw new RuntimeException("Cannot have a non-empty list of end tags.");
        }

        for (int i = 0; i < size; i++) {
            NbtTag tag = NbtTag.fromId(this.listTypeId);
            tag.read(dataInput);
            this.value[i] = tag;
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeByte(this.listTypeId);
        dataOutput.writeInt(this.value.length);

        for (NbtTag tag : this.value) {
            tag.write(dataOutput);
        }
    }
}
