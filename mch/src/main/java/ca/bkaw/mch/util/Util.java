package ca.bkaw.mch.util;

import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Util {
    public static final String NETHER_FOLDER = "DIM-1";
    public static final String THE_END_FOLDER = "DIM1";

    private Util() {}

    /**
     * Get the path where the dimension is stored.
     * <p>
     * Note that {@link Dimension#OVERWORLD} will return the world path.
     *
     * @param worldPath The world path.
     * @param dimensionKey The dimension key.
     * @return The dimension path.
     */
    public static Path getDimensionPath(Path worldPath, String dimensionKey) {
        return switch (dimensionKey) {
            case Dimension.OVERWORLD -> worldPath;
            case Dimension.NETHER -> worldPath.resolve(NETHER_FOLDER);
            case Dimension.THE_END -> worldPath.resolve(THE_END_FOLDER);
            default -> worldPath.resolve("dimensions").resolve(dimensionKey.replace(':', File.separatorChar));
        };
    }

    /**
     * Get the path where mch stores the mch region files and region storage files for
     * the specified dimension of a world.
     *
     * @param repository The repository.
     * @param trackedWorld The world being tracked.
     * @param dimensionKey The dimension.
     * @return The mch region folder path.
     */
    public static Path getMchRegionFolderPath(MchRepository repository, TrackedWorld trackedWorld, String dimensionKey) {
        return repository.getRoot()
            .resolve("world")
            .resolve(trackedWorld.getId().asHex())
            .resolve("dimensions")
            .resolve(dimensionKey.replace(':', '_'))
            .resolve("region");
    }

    /**
     * Format a region file name.
     *
     * @param regionX The region x coordinate.
     * @param regionZ The region z coordinate.
     * @param extension The file extension, including the dot.
     * @return The file name.
     */
    public static String formatRegionFileName(int regionX, int regionZ, String extension) {
        return "r." + regionX + "." + regionZ + extension;
    }

    /**
     * Safely rename a file by keeping both files until the rename operation has been
     * completed.
     * <p>
     * This ensures that if the process were to be terminated at an unfortunate timing,
     * the old or the new file should still be available in their entirety.
     *
     * @param from The file to rename.
     * @param to The new name of the file, or the name of the file to overwrite.
     * @throws IOException If an I/O error occurs.
     */
    public static void safeReplace(Path from, Path to) throws IOException {
        Path oldFilePath = to.getParent().resolve(to.getFileName() + "_old");
        if (Files.exists(to)) {
            Files.move(to, oldFilePath);
        }
        Files.move(from, to);
        Files.deleteIfExists(oldFilePath);
    }

    /**
     * Remove a slash at the end of the string, if there is one.
     *
     * @param str The string.
     * @return The string without a trailing slash.
     */
    public static String noTrailingSlash(String str) {
        if (str.endsWith("/")) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * Add a slash at the end of the string, if there is none.
     *
     * @param str The string.
     * @return The string with a trailing slash.
     */
    public static String trailingSlash(String str) {
        if (!str.endsWith("/")) {
            return str + "/";
        }
        return str;
    }

    /**
     * Remove a slash at the start of the string, if there is one.
     *
     * @param str The string.
     * @return The string without a leading slash.
     */
    public static String noLeadingSlash(String str) {
        if (str.startsWith("/")) {
            return str.substring(1);
        }
        return str;
    }
}
