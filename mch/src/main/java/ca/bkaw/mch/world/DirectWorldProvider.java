package ca.bkaw.mch.world;

import ca.bkaw.mch.object.dimension.Dimension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A world provider that reads from the a {@link Path}.
 * <p>
 * This is used when the world to track is on the same computer as the mch repository.
 */
public class DirectWorldProvider implements WorldProvider {
    public static final String NETHER_FOLDER = "DIM-1";
    public static final String THE_END_FOLDER = "DIM1";

    private final Path path;

    public DirectWorldProvider(Path path) {
        this.path = path;
    }

    @Override
    public List<String> getDimensions() {
        List<String> dimensions = new ArrayList<>(3);
        if (Files.isDirectory(this.path.resolve("region"))) {
            dimensions.add(Dimension.OVERWORLD);
        }
        if (Files.isDirectory(this.path.resolve(NETHER_FOLDER))) {
            dimensions.add(Dimension.NETHER);
        }
        if (Files.isDirectory(this.path.resolve(THE_END_FOLDER))) {
            dimensions.add(Dimension.THE_END);
        }
        return dimensions;
    }
}
