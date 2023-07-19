package ca.bkaw.mch.chunk.parts;

import java.io.DataInput;
import java.io.IOException;

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

    public abstract ChunkDataPartStorage createStorage();

    public abstract ChunkDataPartStorage readStorage(DataInput dataInput) throws IOException;
}
