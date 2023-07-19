package ca.bkaw.mch.chunk.parts;

import ca.bkaw.mch.nbt.NbtCompound;

import java.io.DataOutput;
import java.io.IOException;

/**
 * An object responsible for storing a particular part of an nbt compound.
 */
public interface ChunkDataPartStorage {
    /**
     * Store the part of the nbt.
     *
     * @param chunk The full chunk nbt.
     * @return The version number the data part was stored as.
     */
    int storePart(NbtCompound chunk);

    /**
     * Restore this data part by reading the data part stored with the specified
     * version number and merge the data part into the chunk nbt.
     *
     * @param chunk The chunk nbt.
     * @param versionNumber The version number of this part to restore.
     */
    void restorePart(NbtCompound chunk, int versionNumber);

    /**
     * Write this storage.
     *
     * @param dataOutput The data output to write to.
     * @throws IOException If an I/O error occurs.
     */
    void write(DataOutput dataOutput) throws IOException;
}
