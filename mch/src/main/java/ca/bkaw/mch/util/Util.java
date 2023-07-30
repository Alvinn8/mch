package ca.bkaw.mch.util;

import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;

import java.io.File;
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
        // TODO this should be moved somewhere when we refactor region file names
        return repository.getRoot()
            .resolve("world")
            .resolve(trackedWorld.getId().asHex())
            .resolve("dimensions")
            .resolve(dimensionKey.replace(':', '_'))
            .resolve("region");
    }
}
