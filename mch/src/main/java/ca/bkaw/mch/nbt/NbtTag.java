package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An NBT tag.
 */
public interface NbtTag {
    /**
     * Get the id of this nbt tag type.
     *
     * @return The tag id.
     */
    byte getId();

    /**
     * Read the content of this tag onto this instance.
     * <p>
     * Will not read the tag id or name.
     *
     * @param dataInput The data input to read from.
     */
    void read(DataInput dataInput) throws IOException;

    /**
     * Write the content of this tag.
     * <p>
     * Will only write the content and not the tag id or name.
     *
     * @param dataOutput The data output to write to.
     */
    void write(DataOutput dataOutput) throws IOException;

    /**
     * Create a comparison report that recursively compares the current tag with the
     * other one and produces a human-readable string that describes whether the tags
     * are equal or different and in short describes the changes.
     *
     * @param other The other tag.
     * @return The comparison report.
     */
    String createCompareReport(NbtTag other);

    /**
     * Compare this nbt tag to the other nbt tag.
     *
     * @param other The other tag.
     * @return Whether the tags are equal.
     */
    @Override
    boolean equals(Object other);

    /**
     * Create a new empty nbt tag by tag id.
     * <p>
     * If the id is {@code 0} the {@link NbtEnd#INSTANCE} instance is returned.
     *
     * @param id The id of the tag.
     * @return The newly created nbt tag.
     */
    static NbtTag fromId(byte id) {
        if (id == NbtEnd.ID) return NbtEnd.INSTANCE;
        else if (id == NbtByte.ID) return new NbtByte();
        else if (id == NbtShort.ID) return new NbtShort();
        else if (id == NbtInt.ID) return new NbtInt();
        else if (id == NbtLong.ID) return new NbtLong();
        else if (id == NbtFloat.ID) return new NbtFloat();
        else if (id == NbtDouble.ID) return new NbtDouble();
        else if (id == NbtByteArray.ID) return new NbtByteArray();
        else if (id == NbtString.ID) return new NbtString();
        else if (id == NbtList.ID) return new NbtList();
        else if (id == NbtCompound.ID) return new NbtCompound();
        else if (id == NbtIntArray.ID) return new NbtIntArray();
        else if (id == NbtLongArray.ID) return new NbtLongArray();

        throw new IllegalArgumentException("Unknown nbt tag type id: " + id);
    }

    /**
     * Read nbt from a {@link DataInput}.
     *
     * @param dataInput The data input to read from.
     * @return The tag.
     * @throws IOException If an I/O error occurs.
     */
    static NbtTag readTag(DataInput dataInput) throws IOException {
        byte tagId = dataInput.readByte();
        dataInput.readUTF();
        NbtTag tag = NbtTag.fromId(tagId);
        tag.read(dataInput);
        return tag;
    }

    /**
     * Read nbt where the root tag must be a compound tag.
     *
     * @param dataInput The data input to read from.
     * @return The nbt compound.
     * @throws IOException If an I/O error occurs.
     */
    static NbtCompound readCompound(DataInput dataInput) throws IOException {
        NbtTag tag = readTag(dataInput);
        if (tag instanceof NbtCompound nbtCompound) {
            return nbtCompound;
        }
        throw new RuntimeException("Root tag must be a compound tag.");
    }
}
