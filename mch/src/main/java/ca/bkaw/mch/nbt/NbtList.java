package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

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

    public byte getListTypeId() {
        return this.listTypeId;
    }

    public NbtTag[] getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "NbtList{" +
            "listTypeId=" + listTypeId +
            ", value=" + Arrays.toString(value) +
            '}';
    }

    @Override
    public String createCompareReport(NbtTag tag) {
        NbtList other = (NbtList) tag;
        if (this.value.length != other.value.length) {
            return "DIFF (" + this.value.length + " tags, " + other.value.length + " tags)";
        }
        StringBuilder str = new StringBuilder("List compare report:\n");
        for (int i = 0; i < this.value.length; i++) {
            NbtTag thisTag = this.value[i];
            NbtTag otherTag = other.value[i];
            str.append(i).append(": ").append(thisTag.createCompareReport(otherTag)).append('\n');
        }
        return str.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != NbtList.class) {
            return false;
        }
        NbtList other = (NbtList) obj;
        return this.listTypeId == other.listTypeId && Arrays.equals(this.value, other.value);
    }
}
