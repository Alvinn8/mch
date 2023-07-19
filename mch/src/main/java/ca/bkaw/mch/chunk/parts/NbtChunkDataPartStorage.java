package ca.bkaw.mch.chunk.parts;

import ca.bkaw.mch.NbtStorage;
import ca.bkaw.mch.nbt.NbtCompound;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link ChunkDataPartStorage} that stores the data part as nbt using an
 * {@link NbtStorage}.
 */
public abstract class NbtChunkDataPartStorage implements ChunkDataPartStorage {
    private final NbtStorage nbtStorage;

    public NbtChunkDataPartStorage(DataInput dataInput) throws IOException {
        this.nbtStorage = new NbtStorage(dataInput);
    }

    public NbtChunkDataPartStorage() {
        this.nbtStorage = new NbtStorage();
    }

    /**
     * Extract a part of from the chunk nbt.
     * <p>
     * The extracted nbt compound is the data part nbt that will be stored using the
     * nbt storage.
     *
     * @param chunk The chunk nbt.
     * @return The extracted data part.
     */
    protected abstract NbtCompound extract(NbtCompound chunk);

    @Override
    public int storePart(NbtCompound chunk) {
        // Extract the part of the nbt to store
        NbtCompound dataPartNbt = this.extract(chunk);

        // Store the data part and return the version number it is stored as
        return this.nbtStorage.store(dataPartNbt);
    }

    @Override
    public void restorePart(NbtCompound chunk, int versionNumber) {
        // Read the data part nbt
        NbtCompound dataPartNbt = this.nbtStorage.get(versionNumber);

        // Merge the part into the chunk
        chunk.merge(dataPartNbt);
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        this.nbtStorage.write(dataOutput);
    }
}
