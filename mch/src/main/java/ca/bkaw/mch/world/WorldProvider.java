package ca.bkaw.mch.world;

import ca.bkaw.mch.util.RandomAccessReader;

import java.io.IOException;
import java.util.List;

/**
 * An object that provides mch with information about a world.
 */
public interface WorldProvider {
    List<String> getDimensions() throws IOException;
    List<RegionFileInfo> getRegionFiles(String dimension) throws IOException;
    RandomAccessReader openRegionFile(String dimension, String regionFileName) throws IOException;
}
