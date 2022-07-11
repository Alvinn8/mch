package ca.bkaw.mch.bukkit.provider;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.regionfile.RegionFile;
import ca.bkaw.mch.provider.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * An mch provider for the bukkit platform.
 * <p>
 * Works a bit differently to respect bukkit's world format.
 */
public class BukkitProvider implements Provider {
    private final @NotNull Path overworldFolder;
    private final @Nullable Path netherFolder;
    private final @Nullable Path endFolder;

    public BukkitProvider(@NotNull Path overworldFolder, @Nullable Path netherFolder, @Nullable Path endFolder) {
        this.overworldFolder = overworldFolder;
        this.netherFolder = netherFolder;
        this.endFolder = endFolder;
    }

    @Override
    public List<String> getDimensions() {
        List<String> dimensions = new ArrayList<>(3);
        dimensions.add(Dimension.OVERWORLD);
        if (this.netherFolder != null) {
            dimensions.add(Dimension.NETHER);
        }
        if (this.endFolder != null) {
            dimensions.add(Dimension.THE_END);
        }
        return dimensions;
    }

    private Path getWorldFolder(String dimension) {
        if (Dimension.OVERWORLD.equals(dimension)) {
            return this.overworldFolder;
        }
        if (Dimension.NETHER.equals(dimension)) {
            if (this.netherFolder == null) {
                throw new IllegalArgumentException("This world has no nether dimension.");
            }
            return this.netherFolder;
        }
        if (Dimension.THE_END.equals(dimension)) {
            if (this.endFolder == null) {
                throw new IllegalArgumentException("This world has no end dimension.");
            }
            return this.endFolder;
        }
        throw new IllegalArgumentException("Unknown dimension: \"" + dimension + '"');
    }

    @Override
    public List<String> getRegionFiles(String dimension) throws IOException {
        Path worldFolder = this.getWorldFolder(dimension);
        Path regionFolder = worldFolder.resolve(RegionFile.REGION_FOLDER);
        try (Stream<Path> stream = Files.list(regionFolder)) {
            return stream
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(path -> path.endsWith(".mca"))
                .toList();
        }
    }

    @Override
    public Sha1 getRegionFileHash(String dimension, String regionFile) throws IOException {
        Path worldFolder = this.getWorldFolder(dimension);
        Path regionFolder = worldFolder.resolve(RegionFile.REGION_FOLDER);
        Path regionFilePath = regionFolder.resolve(regionFile);
        return Sha1.ofFile(regionFilePath);
    }
}
