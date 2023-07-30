package ca.bkaw.mch.region;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.chunk.ChunkStorage;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.region.mc.McRegionFileReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A visitor that can read and write chunks from an mch region file in a streaming
 * way, preventing the need to load the entire mch region file into memory at once.
 */
@FunctionalInterface
public interface MchRegionFileVisitor {
    /**
     * Visit a chunk in an mch region file.
     * <p>
     * The visitor may read the current chunk and may store a new version of the chunk.
     * After this method is called the chunk will be saved to the mch region file,
     * including the new changes made by this method.
     *
     * @param chunk The chunk.
     * @throws IOException Throw if an I/O error occurs. An IOException thrown from this
     * method will cause the visit to stop and the {@link #visit(Path,
     * MchRegionFileVisitor)} or {@link #visitReadOnly(Path, MchRegionFileVisitor)}
     * method will throw the IOException.
     */
    void visit(MchRegionFileVisitor.Chunk chunk) throws IOException;

    /**
     * Visit an mch region file for reading and writing.
     *
     * @param mchRegionFilePath The path to the mch region file.
     * @param visitor The visitor.
     * @throws IOException If an I/O error occurs.
     */
    static void visit(Path mchRegionFilePath, MchRegionFileVisitor visitor) throws IOException {
        performVisit(mchRegionFilePath, false, visitor);
    }

    /**
     * Visit an mch region file for reading.
     *
     * @param mchRegionFilePath The mch region file.
     * @param visitor The visitor.
     * @throws IOException If an I/O error occurs.
     */
    static void visitReadOnly(Path mchRegionFilePath, MchRegionFileVisitor visitor) throws IOException {
        performVisit(mchRegionFilePath, true, visitor);
    }

    private static void performVisit(Path path, boolean readOnly, MchRegionFileVisitor visitor) throws IOException {
        Path tempOutputFile = readOnly ? null
            : Files.createTempFile(path.getParent(), path.getFileName().toString(), ".mchr-temp");

        try (
            DataInputStream input = Files.exists(path)
                ? new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))
                : null;
            DataOutputStream output = !readOnly
                ? new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(tempOutputFile)))
                : null
        ) {
            performVisit(input, output, visitor);
        }

        if (tempOutputFile != null) {
            Path oldFilePath = path.getParent().resolve(path.getFileName() + "_old");
            if (Files.exists(path)) {
                Files.move(path, oldFilePath);
            }
            Files.move(tempOutputFile, path);
            Files.deleteIfExists(oldFilePath);
        }
    }

    private static void performVisit(
        @Nullable DataInputStream input,
        @Nullable DataOutputStream output,
        @NotNull MchRegionFileVisitor visitor
    ) throws IOException {
        if (input != null) {
            FileMagic.validate(input, FileMagic.REGION_STORAGE);
            int mchVersion = input.readInt();
            MchVersion.validate(mchVersion, 3);
        }
        if (output != null) {
            output.writeInt(FileMagic.REGION_STORAGE);
            output.writeInt(MchVersion.VERSION_NUMBER);
        }
        int index = 0;
        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
            for (int chunkX = 0; chunkX < 32; chunkX++) {
                // The order is important here.

                int chunkLastModified = input != null ? input.readInt() : 0;
                ChunkStorage chunkStorage = input != null ? new ChunkStorage(input) : new ChunkStorage();

                Chunk chunk = new Chunk(index, chunkX, chunkZ, chunkLastModified, chunkStorage, output == null);

                // Propagate IOException to method
                visitor.visit(chunk);

                // The visitor may now have modified the last modified time and chunk storage.

                if (output != null) {
                    output.writeInt(chunk.lastModified);
                    chunk.chunkStorage.write(output);
                }

                index++;
            }
        }
    }

    /**
     * A chunk that is being visited in a {@link MchRegionFileVisitor}.
     * <p>
     * This object should not be stored and is intended to be garbage collected as
     * soon as the visit has ended. That way the entire contents of an mch region file
     * does not need to be loaded into memory at once.
     */
    class Chunk {
        private final int index, chunkX, chunkZ;
        private final ChunkStorage chunkStorage;
        private final boolean readOnly;
        private int lastModified;

        public Chunk(int index, int chunkX, int chunkZ, int lastModified, ChunkStorage chunkStorage, boolean readOnly) {
            this.index = index;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.lastModified = lastModified;
            this.chunkStorage = chunkStorage;
            this.readOnly = readOnly;
        }

        /**
         * Store a new version of the chunk.
         * <p>
         * This new version will be referenced and used as the latest version in the mch
         * region file.
         *
         * @param chunk The chunk nbt.
         */
        public int store(NbtCompound chunk) {
            if (this.readOnly) {
                throw new IllegalStateException("Can not store a chunk when using visitReadOnly");
            }
            return this.chunkStorage.store(chunk);
        }

        /**
         * Restore the chunk nbt at the specified version number.
         *
         * @param versionNumber The chunk version number.
         * @return The chunk nbt at that snapshot.
         */
        public NbtCompound restore(int versionNumber) {
            return this.chunkStorage.restore(versionNumber);
        }

        /**
         * Get the chunk x coordinate, relative to the region file.
         *
         * @return The chunk coordinate relative to the region file [0-31].
         */
        public int getChunkX() {
            return this.chunkX;
        }

        /**
         * Get the chunk z coordinate, relative to the region file.
         *
         * @return The chunk coordinate relative to the region file [0-31].
         */
        public int getChunkZ() {
            return this.chunkZ;
        }

        /**
         * Get the index in an mch region file where the chunk is located.
         *
         * @return The index.
         */
        public int getIndex() {
            return this.index;
        }

        /**
         * Get when this chunk was marked as last modified, in epoch seconds.
         * <p>
         * This can be compared to the last modified time in the {@link McRegionFileReader}
         * header by reading {@link McRegionFileReader#getChunkLastModified(int, int)}.
         *
         * @return The last modified time.
         */
        public int getLastModified() {
            return this.lastModified;
        }

        /**
         * Set when this chunk is marked as last modified, in epoch seconds.
         *
         * @param lastModified The last modified time.
         */
        public void setLastModified(int lastModified) {
            if (this.readOnly) {
                throw new IllegalStateException("Can not set last modified time when using visitReadOnly");
            }
            this.lastModified = lastModified;
        }
    }
}
