package ca.bkaw.mch.chunk;

import ca.bkaw.mch.chunk.parts.ChunkDataPart;
import ca.bkaw.mch.chunk.parts.ChunkDataParts;
import ca.bkaw.mch.nbt.NbtCompound;

import java.io.DataInput;
import java.io.IOException;

public class MchChunkStorage {
    private final VersionNumberIndex chunkOffsets;

    public MchChunkStorage(DataInput dataInput) throws IOException {
        this.chunkOffsets = new VersionNumberIndex(dataInput);
    }

    public MchChunkStorage() {
        this.chunkOffsets = new VersionNumberIndex();
    }

    public void store(NbtCompound chunk) {
        for (ChunkDataPart chunkDataPart : ChunkDataParts.CHUNK_DATA_PARTS) {
            NbtCompound dataPartNbt = chunkDataPart.extract(chunk);
        }
    }
}
