package ca.bkaw.mch.nbt;

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
}
