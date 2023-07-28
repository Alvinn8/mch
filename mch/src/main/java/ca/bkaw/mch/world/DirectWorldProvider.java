package ca.bkaw.mch.world;

import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.util.RandomAccessReader;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

    public DirectWorldProvider(DataInput dataInput) throws IOException {
        String str = dataInput.readUTF();
        this.path = Path.of(str);
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.path.toAbsolutePath().toString());
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
        // TODO custom dimensions
        return dimensions;
    }

    private Path getDimensionPath(String dimension) {
        return switch (dimension) {
            case Dimension.OVERWORLD -> this.path;
            case Dimension.NETHER -> this.path.resolve(NETHER_FOLDER);
            case Dimension.THE_END -> this.path.resolve(THE_END_FOLDER);
            default -> this.path.resolve("dimensions").resolve(dimension.replace(':', File.separatorChar));
        };
    }

    private long tryGetLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<RegionFileInfo> getRegionFiles(String dimension) throws IOException {
        Path regionFolder = this.getDimensionPath(dimension).resolve("region");
        if (!Files.isDirectory(regionFolder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(regionFolder)) {
            return files
                .filter(path -> path.getFileName().toString().startsWith("r."))
                .filter(path -> path.toString().endsWith(".mca"))
                .map(path -> new RegionFileInfo(path.getFileName().toString(), tryGetLastModifiedTime(path)))
                .toList();
        }
    }

    @Override
    public RandomAccessReader openRegionFile(String dimension, String regionFileName) throws IOException {
        Path path = this.getDimensionPath(dimension).resolve("region").resolve(regionFileName);
        return RandomAccessReader.of(path);
    }

    @Override
    public Stream<String> list(String directory) throws IOException {
        try (Stream<Path> files = Files.list(this.path.resolve(directory))) {
            return files.map(Path::toString);
        }
    }

    @Override
    public byte[] readFile(String fileName) throws IOException {
        return Files.readAllBytes(this.path.resolve(fileName));
    }
}
