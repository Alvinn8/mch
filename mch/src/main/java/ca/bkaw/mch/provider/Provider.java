package ca.bkaw.mch.provider;

import ca.bkaw.mch.Sha1;

import java.io.IOException;
import java.util.List;

/**
 * Provides information about a world to mch.
 */
public interface Provider {
    /**
     * Get a list of dimensions in this world.
     *
     * @return The list of dimensions.
     */
    List<String> getDimensions();

    /**
     * Get a list of region files in a dimension.
     *
     * @param dimension The dimension to get files from.
     * @return The list of region file names, including file extension.
     * @throws IOException If an I/O error occurs.
     */
    List<String> getRegionFiles(String dimension) throws IOException;

    /**
     * Get the SHA-1 hash of a region file.
     *
     * @param dimension The dimension the region file is in.
     * @param regionFile The region file name.
     * @return The SHA-1 hash.
     * @throws IOException If an I/O error occurs.
     */
    Sha1 getRegionFileHash(String dimension, String regionFile) throws IOException;
}
