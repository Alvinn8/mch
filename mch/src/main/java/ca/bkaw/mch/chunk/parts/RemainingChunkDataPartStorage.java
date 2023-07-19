package ca.bkaw.mch.chunk.parts;

import ca.bkaw.mch.nbt.NbtCompound;

import java.io.DataInput;
import java.io.IOException;

/**
 * The {@link NbtChunkDataPartStorage} that holds the remaining chunk data.
 */
public class RemainingChunkDataPartStorage extends NbtChunkDataPartStorage {
    public RemainingChunkDataPartStorage(DataInput dataInput) throws IOException {
        super(dataInput);
    }

    public RemainingChunkDataPartStorage() {}

    @Override
    protected NbtCompound extract(NbtCompound chunk) {
        // Everything is extracted. Note that the same instance is reused here. The
        // chunk compound is modified by each ChunkDataPart anyway so caution is already
        // taken to not use the chunk nbt compound afterwards as it has been mutated.
        return chunk;
    }
}
