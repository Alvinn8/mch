package ca.bkaw.mch.world;

import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.blob.Blob;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.util.RandomAccessReader;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/**
 * An object that provides mch with information about a world.
 */
public interface WorldProvider {
    /**
     * Get the dimensions that this world has.
     *
     * @return The list of dimension keys.
     * @throws IOException If an I/O error occurs.
     */
    List<String> getDimensions() throws IOException;

    /**
     * Get the region files of a dimension.
     *
     * @param dimension The dimension key.
     * @return The list of {@link RegionFileInfo information about region files}.
     * @throws IOException If an I/O error occurs.
     */
    List<RegionFileInfo> getRegionFiles(String dimension) throws IOException;

    /**
     * Open a region file in a dimension for reading.
     *
     * @param dimension The dimension key.
     * @param regionFileName The file name of the region file.
     * @return The reader.
     * @throws IOException If an I/O error occurs.
     */
    RandomAccessReader openRegionFile(String dimension, String regionFileName) throws IOException;

    /**
     * Track a directory of files by creating {@link Tree} and {@link Blob} objects to
     * represent the current version of the directory.
     *
     * @param repository The repository to save to.
     * @param dimension The dimension to track.
     * @param predicate A predicate that controls which files to include.
     * @return The reference to the created {@link Tree} object.
     * @throws IOException If an I/O error occurs.
     */
    Reference20<Tree> trackDirectoryTree(String dimension, MchRepository repository, Predicate<String> predicate) throws IOException;
}
