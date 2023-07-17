package ca.bkaw.mch.chunk;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An index that maps a version number identifier to the offset of the object.
 */
public class VersionNumberIndex {
    private final Map<Integer, Integer> map;

    public VersionNumberIndex(DataInput dataInput) throws IOException {
        int length = dataInput.readInt();
        this.map = new HashMap<>(length);
        for (int i = 0; i < length; i++) {
            int versionNumber = dataInput.readInt();
            int offset = dataInput.readInt();
            this.map.put(versionNumber, offset);
        }
    }

    public VersionNumberIndex() {
        this.map = new HashMap<>();
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(this.map.size());
        for (Map.Entry<Integer, Integer> entry : this.map.entrySet()) {
            Integer versionNumber = entry.getKey();
            dataOutput.writeInt(versionNumber);
            Integer offset = entry.getValue();
            if (offset == null) {
                throw new IllegalStateException("Can not write the index as the offset for version number " + versionNumber + " has not been set.");
            }
            dataOutput.writeInt(offset);
        }
    }

    /**
     * Get the offset where the specified object is located.
     *
     * @param versionNumber The version number to look for.
     * @return The byte offset.
     */
    public int getOffset(int versionNumber) {
        Integer offset = this.map.get(versionNumber);
        if (offset == null) {
            throw new RuntimeException("Chunk version number " + versionNumber + " was not present in the index.");
        }
        return offset;
    }

    /**
     * Set the offset for the specified version number.
     *
     * @param versionNumber The version number.
     * @param offset The offset.
     */
    public void setOffset(int versionNumber, int offset) {
        this.map.put(versionNumber, offset);
    }

    /**
     * Add a version number to the index, without specifying its offset.
     * <p>
     * This is necesary to ensure the correct {@link #byteSize} is calculated.
     *
     * @param versionNumber The version number.
     */
    public void addToIndex(int versionNumber) {
        this.map.put(versionNumber, null);
    }

    /**
     * Get the size in bytes to store this index.
     *
     * @return The amount of bytes.
     */
    public int byteSize() {
        return 4 + this.map.size() * 8;
    }
}
