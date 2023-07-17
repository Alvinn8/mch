package ca.bkaw.mch.nbt;

import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A compound tag that holds string keys mapped to nbt tags.
 */
public class NbtCompound implements NbtTag {
    public static final byte ID = 10;

    private final Map<String, NbtTag> data = new LinkedHashMap<>();

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        byte tagId;
        while ((tagId = dataInput.readByte()) != NbtEnd.ID) {
            NbtTag tag = NbtTag.fromId(tagId);
            String name = dataInput.readUTF();
            tag.read(dataInput);
            this.data.put(name, tag);
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        for (Map.Entry<String, NbtTag> entry : this.data.entrySet()) {
            String name = entry.getKey();
            NbtTag tag = entry.getValue();

            dataOutput.writeByte(tag.getId());
            dataOutput.writeUTF(name);
            tag.write(dataOutput);
        }
        // End of compound
        dataOutput.writeByte(NbtEnd.ID);
    }

    @Nullable
    public NbtTag get(String key) {
        return this.data.get(key);
    }

    public void remove(String key) {
        this.data.remove(key);
    }

    @Override
    public String toString() {
        return "NbtCompound" + data;
    }

    @Override
    public String createCompareReport(NbtTag tag) {
        NbtCompound other = (NbtCompound) tag;
        StringBuilder str = new StringBuilder("Compound compare report:\n");
        for (Map.Entry<String, NbtTag> entry : this.data.entrySet()) {
            String key = entry.getKey();
            NbtTag thisTag = entry.getValue();
            NbtTag otherTag = other.get(key);
            str.append(key).append(": ").append(thisTag.createCompareReport(otherTag)).append('\n');
        }
        return str.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != NbtCompound.class) {
            return false;
        }
        return this.data.equals(((NbtCompound) obj).data);
    }
}
