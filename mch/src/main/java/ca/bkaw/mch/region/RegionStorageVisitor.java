package ca.bkaw.mch.region;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.chunk.ChunkStorage;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.region.mc.McRegionFileReader;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.util.Util;
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
 * A visitor that can read and write chunks from an mch region storage file in a
 * streaming way, preventing the need to load the entire mch region storage into
 * memory at once.
 * <p>
 * The region storage contains the {@link ChunkStorage}s that store the different
 * version numbers of chunks. For the files that store the chunk version numbers
 * to use for a specific region file version number, see {@link MchRegionFile}.
 *
 * @see MchRegionFile
 */
@FunctionalInterface
public interface RegionStorageVisitor {
    int MAGIC = FileMagic.REGION_STORAGE;

    /**
     * Visit a chunk in an mch region storage file.
     * <p>
     * The visitor may read the current chunk and may store a new version of the chunk.
     * After this method is called the chunk will be saved to the mch region storage,
     * including the new changes made by this method.
     *
     * @param chunk The chunk.
     * @throws IOException Throw if an I/O error occurs. An IOException thrown from this
     * method will cause the visit to stop and the {@link #visit(Path,
     * RegionStorageVisitor)} or {@link #visitReadOnly(Path, RegionStorageVisitor)}
     * method will throw the IOException.
     */
    void visit(RegionStorageVisitor.Chunk chunk) throws IOException;

    /**
     * Get the path to where a region storage file will be stored in the repository.
     *
     * @param repository The mch repository.
     * @param trackedWorld The tracked world.
     * @param dimensionKey The dimension of the world.
     * @param regionX The region x coordinate.
     * @param regionZ The region z coordinate.
     * @return The region storage file path.
     */
    static Path getPath(MchRepository repository, TrackedWorld trackedWorld, String dimensionKey, int regionX, int regionZ) {
        Path mchRegionFolderPath = Util.getMchRegionFolderPath(
            repository, trackedWorld, dimensionKey
        );
        String fileName = Util.formatRegionFileName(
            regionX, regionZ, ".mchrs.gz"
        );
        return mchRegionFolderPath.resolve(fileName);
    }

    /**
     * Visit an mch region storage file for reading and writing.
     *
     * @param regionStoragePath The path to the mch region storage file.
     * @param visitor The visitor.
     * @throws IOException If an I/O error occurs.
     */
    static void visit(Path regionStoragePath, RegionStorageVisitor visitor) throws IOException {
        performVisit(regionStoragePath, false, visitor);
    }

    /**
     * Visit an mch region file for reading.
     *
     * @param regionStoragePath The mch region file.
     * @param visitor The visitor.
     * @throws IOException If an I/O error occurs.
     */
    static void visitReadOnly(Path regionStoragePath, RegionStorageVisitor visitor) throws IOException {
        performVisit(regionStoragePath, true, visitor);
    }

    private static void performVisit(Path path, boolean readOnly, RegionStorageVisitor visitor) throws IOException {
        Path tempOutputFile = readOnly ? null
            : Files.createTempFile(path.getParent(), path.getFileName().toString(), ".mchrs-temp");

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
            Util.safeReplace(tempOutputFile, path);
        }
    }

    private static void performVisit(
        @Nullable DataInputStream input,
        @Nullable DataOutputStream output,
        @NotNull RegionStorageVisitor visitor
    ) throws IOException {
        if (input != null) {
            FileMagic.validate(input, MAGIC);
            int mchVersion = input.readInt();
            MchVersion.validate(mchVersion, 3);
        }
        if (output != null) {
            output.writeInt(MAGIC);
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
     * A chunk that is being visited in a {@link RegionStorageVisitor}.
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
