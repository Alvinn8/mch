package ca.bkaw.mch.region;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Terrible class name but this is the region files that reference what chunk
 * version numbers to use to recreate a certain version of a region file.
 */
public class MchRefRegionFile {
    public static final int MAGIC = FileMagic.REGION_FILE;

    public static int createUghImTired(Path path, int[] newChunkVersionNumbers) throws IOException {
        if (newChunkVersionNumbers.length != 1024) {
            throw new IllegalArgumentException("Expected 1024 chunk version numbers");
        }
        Path tempOutputFile = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".mchrv-temp");

        int newRegionFileVersionNumber;
        try (
            DataInputStream input = Files.exists(path)
                ? new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))
                : null;
            DataOutputStream output = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(tempOutputFile)))
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
                while (input.readBoolean()) {
                    output.writeBoolean(true);
                    int regionFileVersionNumber = input.readInt();
                    output.writeInt(regionFileVersionNumber);
                    int[] chunkVersionNumbers = new int[1024];
                    for (int j = 0; j < 1024; j++) {
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
                nextRegionFileVersionNumber = input.readInt();
            }
            // We need to add a new region file version number to the file
            newRegionFileVersionNumber = nextRegionFileVersionNumber;
            output.writeBoolean(true);
            output.writeInt(newRegionFileVersionNumber);
            for (int i = 0; i < 1024; i++) {
                output.writeInt(newChunkVersionNumbers[i]);
            }

            nextRegionFileVersionNumber++;
            output.writeBoolean(false);
            output.writeInt(nextRegionFileVersionNumber);
        }

        Path oldFilePath = path.getParent().resolve(path.getFileName() + "_old");
        if (Files.exists(path)) {
            Files.move(path, oldFilePath);
        }
        Files.move(tempOutputFile, path);
        Files.deleteIfExists(oldFilePath);

        return newRegionFileVersionNumber;
    }

    public static int[] read(Path path, int regionFileVersionNumber) throws IOException {
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
            FileMagic.validate(input, MAGIC);
            int mchVersion = input.readInt();
            MchVersion.validate(mchVersion, 3);
            while (input.readBoolean()) {
                int regionFileVersionNumberRead = input.readInt();
                if (regionFileVersionNumberRead == regionFileVersionNumber) {
                    int[] chunkVersionNumbers = new int[1024];
                    for (int i = 0; i < 1024; i++) {
                        chunkVersionNumbers[i] = input.readInt();
                    }
                    return chunkVersionNumbers;
                }
                input.skipNBytes(1024 * Integer.BYTES);
            }
            throw new RuntimeException("Region file version number " + regionFileVersionNumber + " was not present in the file.");
        }
    }
}
