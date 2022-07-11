package ca.bkaw.mch.object.regionfile;

import ca.bkaw.mch.object.Reference20;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Information about a chunk from inside a region file.
 * <p>
 * References the actual {@link ca.bkaw.mch.object.chunk.Chunk} object.
 */
public class RegionFileChunk implements Comparable<RegionFileChunk> {
    private final int chunkX;
    private final int chunkY;
    private final Reference20 chunkRef;

    public RegionFileChunk(int chunkX, int chunkY, Reference20 chunkRef) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkRef = chunkRef;
    }

    public RegionFileChunk(DataInputStream stream) throws IOException {
        this.chunkX = stream.readInt();
        this.chunkY = stream.readInt();
        this.chunkRef = Reference20.read(stream);
    }

    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeInt(this.chunkX);
        stream.writeInt(this.chunkY);
        this.chunkRef.write(stream);
    }

    @Override
    public int compareTo(RegionFileChunk o) {
        int x = Integer.compare(this.chunkX, o.chunkX);
        if (x != 0) {
            return x;
        }
        return Integer.compare(this.chunkY, o.chunkY);
    }

    public int getChunkX() {
        return this.chunkX;
    }

    public int getChunkY() {
        return this.chunkY;
    }

    public Reference20 getChunkRef() {
        return this.chunkRef;
    }
}
