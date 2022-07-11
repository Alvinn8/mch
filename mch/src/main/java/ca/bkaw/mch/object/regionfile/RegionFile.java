package ca.bkaw.mch.object.regionfile;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.StorageObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RegionFile extends StorageObject {
    public static final String REGION_FOLDER = "region";

    /**
     * The SHA-1 hash to identify the actual region file on disk.
     * @see #getRegionFileIdentifier(String, String, Sha1)
     */
    private final Sha1 regionFileIdentifier;
    private final List<RegionFileChunk> chunks;

    public RegionFile(Sha1 regionFileIdentifier, List<RegionFileChunk> chunks) {
        this.regionFileIdentifier = regionFileIdentifier;
        this.chunks = chunks;
    }

    public RegionFile(DataInputStream stream) throws IOException {
        byte[] bytes = new byte[20];
        stream.readFully(bytes);
        this.regionFileIdentifier = new Sha1(bytes);

        int size = stream.readInt();
        this.chunks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.chunks.add(new RegionFileChunk(stream));
        }
    }

    /**
     * Get the SHA-1 hash to identify a region file on disk.
     *
     * @param dimension The dimension of the region file.
     * @param fileName The file name of the region file.
     * @param fileHash The SHA-1 hash of the file contents.
     * @return The SHA-1 hash.
     */
    public static Sha1 getRegionFileIdentifier(String dimension, String fileName, Sha1 fileHash) {
        return Sha1.of(dimension, fileName, fileHash);
    }

    @Override
    public void serialize(DataOutputStream stream) throws IOException {
        stream.write(this.regionFileIdentifier.getBytes());

        stream.writeInt(this.chunks.size());
        // Sort entries to ensure result is always the same
        for (RegionFileChunk regionFileChunk : this.chunks) {
            regionFileChunk.serialize(stream);
        }
    }

    @Override
    public Sha1 getSha1Hash(Path tempFile) {
        return this.regionFileIdentifier;
    }

    @Override
    public String cat() {
        StringBuilder str = new StringBuilder("chunks:");
        for (RegionFileChunk chunk : this.chunks) {
            str.append("    ");
            str.append(chunk.getChunkX());
            str.append(' ');
            str.append(chunk.getChunkY());
            str.append(": ");
            str.append(chunk.getChunkRef().getSha1().asHex());
        }
        return str.toString();
    }
}
