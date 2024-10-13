package ca.bkaw.mch.region;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.region.mc.McRegionFileReader;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.util.Util;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * The file that stores the chunk version numbers to use for a specific region file
 * version number.
 * <p>
 * Random access is not implemented for performance reasons. Therefore, this class
 * offers streaming methods for reading and writing mch region files.
 *
 * @see RegionStorageVisitor
 */
public class MchRegionFile {
    public static final int MAGIC = FileMagic.REGION_FILE;
    public static final int CHUNK_COUNT = McRegionFileReader.CHUNK_COUNT;

    /**
     * Get the path to where an mch region file will be stored in the repository.
     * <p>
     * This file is important and should be handled with care. This method is
     * therefore private to avoid exposing the path.
     *
     * @param repository The mch repository.
     * @param trackedWorld The tracked world.
     * @param dimensionKey The dimension of the world.
     * @param regionX The region x coordinate.
     * @param regionZ The region z coordinate.
     * @return The mch region file path.
     */
    private static Path getPath(MchRepository repository, TrackedWorld trackedWorld, String dimensionKey, int regionX, int regionZ) {
        Path mchRegionFolderPath = Util.getMchRegionFolderPath(
            repository, trackedWorld, dimensionKey
        );
        String fileName = Util.formatRegionFileName(
            regionX, regionZ, ".mchrv.zst"
        );
        return mchRegionFolderPath.resolve(fileName);
    }

    /**
     * Store a new array of chunk version numbers in the specified mch region file.
     * <p>
     * In case the provided array of chunk version numbers is already present in the
     * mch region file, the file will not be modified and the existing region file
     * version number is returned.
     *
     * @param repository The mch repository.
     * @param trackedWorld The tracked world.
     * @param dimensionKey The dimension of the world.
     * @param regionX The region x coordinate.
     * @param regionZ The region z coordinate.
     * @param newChunkVersionNumbers The array of 1024 chunk version numbers.
     * @return The region file version number of the stored chunk version numbers.
     * @throws IOException If an I/O error occurs.
     */
    public static int store(MchRepository repository, TrackedWorld trackedWorld, String dimensionKey, int regionX, int regionZ, int[] newChunkVersionNumbers) throws IOException {
        Path path = getPath(repository, trackedWorld, dimensionKey, regionX, regionZ);
        return store(path, newChunkVersionNumbers);
    }

    @VisibleForTesting
    static int store(Path path, int[] newChunkVersionNumbers) throws IOException {
        if (newChunkVersionNumbers.length != CHUNK_COUNT) {
            throw new IllegalArgumentException("Expected " + CHUNK_COUNT + " chunk version numbers");
        }
        Path tempOutputFile = Files.createTempFile(
            path.getParent(),
            path.getFileName().toString(),
            ".mchrv-temp"
        );

        int newRegionFileVersionNumber;
        try (
            DataInputStream input = Files.exists(path)
                ? new DataInputStream(new BufferedInputStream(new ZstdInputStream(Files.newInputStream(path))))
                : null;
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new ZstdOutputStream(Files.newOutputStream(tempOutputFile))))
        ) {
            if (input != null) {
                FileMagic.validate(input, MAGIC);
                int mchVersion = input.readInt();
                MchVersion.validate(mchVersion, 3);
            }
            output.writeInt(MAGIC);
            output.writeInt(MchVersion.VERSION_NUMBER);
            int nextRegionFileVersionNumber = 1;
            if (input != null) {
                // While there are more chunk version numbers to read
                while (input.readBoolean()) {
                    // Write a true to signal that there is more chunk version numbers to read
                    output.writeBoolean(true);

                    int regionFileVersionNumber = input.readInt();
                    output.writeInt(regionFileVersionNumber);

                    int[] chunkVersionNumbers = new int[CHUNK_COUNT];
                    for (int j = 0; j < CHUNK_COUNT; j++) {
                        int chunkVersionNumber = input.readInt();
                        chunkVersionNumbers[j] = chunkVersionNumber;
                        output.writeInt(chunkVersionNumber);
                    }

                    if (Arrays.equals(chunkVersionNumbers, newChunkVersionNumbers)) {
                        // There is already a region file version number with these exact chunk version
                        // numbers. We can use the existing region file version number.

                        // Delete the temporary output file since no changes were made to the file.
                        output.close();
                        Files.delete(tempOutputFile);

                        // Return the existing version number
                        return regionFileVersionNumber;
                    }
                }
                // False was read which means there are no more chunk version numbers. The only
                // thing left in the file is the next region file version number.
                nextRegionFileVersionNumber = input.readInt();
            }
            // The new chunk version numbers did not already exist in the file.
            // We need to add a new region file version number to the file
            newRegionFileVersionNumber = nextRegionFileVersionNumber;

            // True to signal that there is one more chunk version numbers array to read,
            // the one that is being added.
            output.writeBoolean(true);
            output.writeInt(newRegionFileVersionNumber);
            for (int i = 0; i < CHUNK_COUNT; i++) {
                output.writeInt(newChunkVersionNumbers[i]);
            }

            // False to signal that there are no more chunk version numbers and that the
            // following data is the next region file version number.
            output.writeBoolean(false);

            nextRegionFileVersionNumber++;
            output.writeInt(nextRegionFileVersionNumber);
        }

        Util.safeReplace(tempOutputFile, path);

        return newRegionFileVersionNumber;
    }

    /**
     * Read an array of chunk version numbers to use to restore a region file to the
     * specified region file version number.
     *
     * @param repository The mch repository.
     * @param trackedWorld The tracked world.
     * @param dimensionKey The dimension of the world.
     * @param regionX The region x coordinate.
     * @param regionZ The region z coordinate.
     * @param regionFileVersionNumber The region file version number.
     * @return The chunk version numbers for the specified region file version number.
     * @throws IOException If an I/O error occurs.
     */
    public static int[] read(MchRepository repository, TrackedWorld trackedWorld, String dimensionKey, int regionX, int regionZ, int regionFileVersionNumber) throws IOException {
        Path path = getPath(repository, trackedWorld, dimensionKey, regionX, regionZ);
        return read(path, regionFileVersionNumber);
    }

    @VisibleForTesting
    static int[] read(Path path, int regionFileVersionNumber) throws IOException {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(new ZstdInputStream(Files.newInputStream(path))))) {
            FileMagic.validate(input, MAGIC);
            int mchVersion = input.readInt();
            MchVersion.validate(mchVersion, 3);
            while (input.readBoolean()) {
                int regionFileVersionNumberRead = input.readInt();
                if (regionFileVersionNumberRead != regionFileVersionNumber) {
                    input.skipNBytes(CHUNK_COUNT * Integer.BYTES);
                    continue;
                }
                int[] chunkVersionNumbers = new int[CHUNK_COUNT];
                for (int i = 0; i < CHUNK_COUNT; i++) {
                    chunkVersionNumbers[i] = input.readInt();
                }
                return chunkVersionNumbers;
            }
            throw new RuntimeException("Region file version number " + regionFileVersionNumber + " was not present in the mch region file.");
        }
    }
}
