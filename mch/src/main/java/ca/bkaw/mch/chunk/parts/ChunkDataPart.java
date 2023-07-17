package ca.bkaw.mch.chunk.parts;

import ca.bkaw.mch.nbt.NbtCompound;

public abstract class ChunkDataPart {
    private final int id;

    public ChunkDataPart(int id) {
        this.id = id;
    }

    /**
     * Get the id of this type of data part.
     *
     * @return The id.
     */
    int getId() {
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
