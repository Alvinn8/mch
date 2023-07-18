package ca.bkaw.mch.chunk.parts;

import ca.bkaw.mch.nbt.NbtCompound;

/**
 * The {@link ChunkDataPart} that holds the remaining chunk data.
 */
public class RemainingChunkDataPart extends ChunkDataPart {
    public RemainingChunkDataPart(int id) {
        super((byte) id);
    }

    @Override
    public NbtCompound extract(NbtCompound chunk) {
        // Everything is extracted. Note that the same instance is reused here. The
        // chunk compound is modified by each ChunkDataPart anyway so caution is already
        // taken to not use the chunk nbt compound afterwards as it has been mutated.
        return chunk;
    }

    @Override
    public void merge(NbtCompound chunk, NbtCompound dataPart) {
        chunk.merge(dataPart);
    }
}
