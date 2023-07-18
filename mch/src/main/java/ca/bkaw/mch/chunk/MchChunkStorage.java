package ca.bkaw.mch.chunk;

import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.UnsupportedMchVersionException;
import ca.bkaw.mch.chunk.parts.ChunkDataPart;
import ca.bkaw.mch.chunk.parts.ChunkDataParts;
import ca.bkaw.mch.nbt.NbtCompound;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class MchChunkStorage {
    public static final int MAGIC = 0x4D434821;

    private final VersionNumberIndex chunkOffsets;

    public MchChunkStorage(DataInput dataInput) throws IOException {
        if (dataInput.readInt() != MAGIC) {
            throw new RuntimeException("Expected mch chunk storage magic header. Is the mch chunk file corrupted?");
        }
        int mchVersion = dataInput.readInt();
        if (mchVersion != MchVersion.VERSION_NUMBER) {
            throw new UnsupportedMchVersionException("Chunk file has unsupported mch version " + mchVersion + " only " + MchVersion.VERSION_NUMBER + " is supported.");
        }
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

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MAGIC);
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        this.chunkOffsets.write(dataOutput);
    }
}
