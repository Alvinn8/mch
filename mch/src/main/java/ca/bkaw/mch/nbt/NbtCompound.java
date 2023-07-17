package ca.bkaw.mch.nbt;

import ca.bkaw.mch.test.TestMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

    @NotNull
    public NbtCompound getCompound(String key) {
        NbtTag tag = this.get(key);
        if (tag instanceof NbtCompound compound) {
            return compound;
        }
        throw new RuntimeException("Expected compound as tag " + key + " but found " + tag);
    }

    public void remove(String key) {
        this.data.remove(key);
    }

    public void set(String key, NbtTag tag) {
        this.data.put(key, tag);
    }

    /**
     * Merge the {@code other} compound into this compound.
     *
     * @param other The compound to merge into this one.
     */
    public void merge(NbtCompound other) {
        this.data.putAll(other.data);
    }

    @Override
    public String toString() {
        return "NbtCompound" + this.data;
    }

    @Override
    public String createCompareReport(NbtTag tag) {
        NbtCompound other = (NbtCompound) tag;
        StringBuilder str = new StringBuilder("Compound compare report (");
        str.append(this.equals(tag) ? "EQUAL" : "DIFF");
        str.append(", ");
        str.append(TestMain.formatBytes(this.byteSize()));
        str.append("):\n");
        for (Map.Entry<String, NbtTag> entry : this.data.entrySet()) {
            String key = entry.getKey();
            NbtTag thisTag = entry.getValue();
            NbtTag otherTag = other.get(key);
            str.append(key).append(": ");
            if (otherTag == null) {
                str.append("CREATED\n");
            } else {
                str.append(thisTag.createCompareReport(otherTag)).append('\n');
            }
        }
        if (this.data.size() == 0) {
            str.append("(length 0, length ").append(other.data.size()).append(")");
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

    @Override
    public int byteSize() {
        int count = 0;
        for (Map.Entry<String, NbtTag> entry : this.data.entrySet()) {
            count += 1;
            count += entry.getKey().getBytes(StandardCharsets.UTF_8).length;
            count += entry.getValue().byteSize();
        }
        return count;
    }
}
