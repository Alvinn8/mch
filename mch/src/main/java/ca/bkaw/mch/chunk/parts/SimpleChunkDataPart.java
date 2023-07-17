package ca.bkaw.mch.chunk.parts;

import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;

import java.util.Set;

/**
 * A chunk data part that extracts and merges a set of keys.
 */
public class SimpleChunkDataPart extends ChunkDataPart {
    private final Set<String> keys;

    public SimpleChunkDataPart(int id, Set<String> keys) {
        super(id);
        this.keys = keys;
    }

    @Override
    public NbtCompound extract(NbtCompound chunk) {
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

    @Override
    public void merge(NbtCompound chunk, NbtCompound dataPart) {
        chunk.merge(dataPart);
    }
}
