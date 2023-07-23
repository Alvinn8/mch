package ca.bkaw.mch.region;

import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.chunk.ChunkStorage;
import ca.bkaw.mch.nbt.NbtCompound;
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
     * Visit an mch region file for reading an writing.
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
            Path oldFilePath = path.getParent().resolve(tempOutputFile.getFileName() + "_old");
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
            if (input.readInt() != MchRegionFile.MAGIC) {
                throw new RuntimeException("Expected mch region file magic header. Is the mch region file corrupted?");
            }
            int mchVersion = input.readInt();
            MchVersion.validate(mchVersion, 2);
        }
        if (output != null) {
            output.writeInt(MchRegionFile.MAGIC);
            output.writeInt(MchVersion.VERSION_NUMBER);
        }
        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
            for (int chunkX = 0; chunkX < 32; chunkX++) {
                // The order is important here.

                int chunkVersionNumber = input != null ? input.readInt() : 0;
                ChunkStorage chunkStorage = null;
                if (chunkVersionNumber != 0) {
                    chunkStorage = new ChunkStorage(input);
                }

                Chunk chunk = new Chunk(chunkX, chunkZ, output == null, chunkVersionNumber, chunkStorage);

                // Propagate IOException to method
                visitor.visit(chunk);

                // The visitor may not have modified the version number and chunk storage.

                if (output != null) {
                    output.writeInt(chunk.versionNumber);
                    if (chunk.versionNumber != 0) {
                        chunk.chunkStorage.write(output);
                    }
                }
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
        private final int chunkX, chunkZ;
        private final boolean readOnly;
        private int versionNumber;
        private ChunkStorage chunkStorage;

        public Chunk(int chunkX, int chunkZ, boolean readOnly, int versionNumber, ChunkStorage chunkStorage) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.readOnly = readOnly;
            this.versionNumber = versionNumber;
            this.chunkStorage = chunkStorage;
        }

        /**
         * Check if there is a chunk stored in this mch region file.
         *
         * @return Whether there is a chunk stored.
         */
        public boolean isEmpty() {
            return this.versionNumber == 0;
        }

        /**
         * Read the current version of the chunk.
         *
         * @return The chunk nbt.
         */
        public NbtCompound read() {
            if (this.versionNumber == 0) {
                throw new IllegalStateException("No chunk is available to read.");
            }
            return this.chunkStorage.restore(this.versionNumber);
        }

        /**
         * Store a new version of the chunk.
         * <p>
         * This new version will be referenced and used as the latest version in the mch
         * region file.
         *
         * @param chunk The chunk nbt.
         */
        public void store(NbtCompound chunk) {
            if (this.readOnly) {
                throw new IllegalStateException("Can not store a chunk when using visitReadOnly");
            }
            if (this.chunkStorage == null) {
                this.chunkStorage = new ChunkStorage();
            }
            this.versionNumber = this.chunkStorage.store(chunk);
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
    }
}
