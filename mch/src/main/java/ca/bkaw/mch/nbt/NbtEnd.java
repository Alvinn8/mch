package ca.bkaw.mch.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An NBT end tag that represents the end of an {@link NbtCompound}.
 */
public class NbtEnd implements NbtTag {

    public static final int ID = 0;
    public static final NbtEnd INSTANCE = new NbtEnd();

    private NbtEnd() {}

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void read(DataInput dataInput) throws IOException {
        throw new UnsupportedOperationException("End tags do not have a payload");
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        throw new UnsupportedOperationException("End tags do not have a payload");
    }

    @Override
    public String createCompareReport(NbtTag other) {
        throw new UnsupportedOperationException("End tags can not be compared");
    }
}
