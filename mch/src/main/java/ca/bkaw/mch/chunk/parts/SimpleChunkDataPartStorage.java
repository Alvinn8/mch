package ca.bkaw.mch.chunk.parts;

import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;

import java.io.DataInput;
import java.io.IOException;
import java.util.Set;

/**
 * A {@link ChunkDataPartStorage} that extracts specific keys from the nbt compound.
 */
public class SimpleChunkDataPartStorage extends NbtChunkDataPartStorage {
    private final Set<String> keys;

    public SimpleChunkDataPartStorage(Set<String> keys, DataInput dataInput) throws IOException {
        super(dataInput);
        this.keys = keys;
    }

    public SimpleChunkDataPartStorage(Set<String> keys) {
        super();
        this.keys = keys;
    }

    @Override
    protected NbtCompound extract(NbtCompound chunk) {
        NbtCompound extracted = new NbtCompound();
        for (String key : this.keys) {
            NbtTag tag = chunk.get(key);
            if (tag != null) {
                chunk.remove(key);
                extracted.set(key, tag);
            }
        }
        return extracted;
    }
}
