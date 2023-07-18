package ca.bkaw.mch.chunk.parts;

import ca.bkaw.mch.nbt.NbtCompound;

/**
 * A handler that extracts and merges a part of the nbt data of a chunk.
 * <p>
 * Different instances of this class will split chunk data into different parts
 * that are stored separately. That way only the data part that has changed needs
 * to be saved again.
 *
 * @see ChunkDataParts
 */
public abstract class ChunkDataPart {
    private final byte id;

    public ChunkDataPart(byte id) {
        this.id = id;
    }

    /**
     * Get the id of this type of data part.
     *
     * @return The id.
     */
    public byte getId() {
        return this.id;
    }

    /**
     * Extract the desired data by moving the nbt tags from the chunk nbt into a new
     * tag.
     *
     * @param chunk The chunk nbt.
     * @return The compound containing the extracted data.
     */
    public abstract NbtCompound extract(NbtCompound chunk);

    /**
     * Merge the data part with the chunk nbt.
     *
     * @param chunk The chunk nbt.
     * @param dataPart The data part nbt.
     */
    public abstract void merge(NbtCompound chunk, NbtCompound dataPart);
}
