package ca.bkaw.mch.world;

import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.blob.Blob;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.util.Util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A world provider that reads from the a {@link Path}.
 * <p>
 * This is used when the world to track is on the same computer as the mch repository.
 */
public class DirectWorldProvider implements WorldProvider {
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
        if (Files.isDirectory(this.path.resolve(Util.NETHER_FOLDER))) {
            dimensions.add(Dimension.NETHER);
        }
        if (Files.isDirectory(this.path.resolve(Util.THE_END_FOLDER))) {
            dimensions.add(Dimension.THE_END);
        }
        // TODO custom dimensions
        return dimensions;
    }

    private Path getDimensionPath(String dimension) {
        return Util.getDimensionPath(this.path, dimension);
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
    public Reference20<Tree> trackDirectoryTree(String dimension, MchRepository repository, Predicate<String> predicate) throws IOException {
        return this.trackDirectoryTree(repository, this.getDimensionPath(dimension), predicate);
    }

    private Reference20<Tree> trackDirectoryTree(MchRepository repository, Path directory, Predicate<String> predicate) throws IOException {
        Tree tree = new Tree();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path path : files.sorted().toList()) {
                String name = path.getFileName().toString();
                if (!predicate.test(name)) {
                    continue;
                }
                if (Files.isDirectory(path)) {
                    // Track subdirectories
                    Reference20<Tree> subDirectoryReference = trackDirectoryTree(repository, path, str -> true);
                    tree.addSubTree(name, subDirectoryReference);
                }  else if (Files.isRegularFile(path)) {
                    // Track files
                    byte[] bytes = Files.readAllBytes(path);
                    Blob blob = new Blob(bytes);
                    tree.addFile(name, ObjectStorageTypes.BLOB.save(blob, repository));
                }
            }
        }

        // Save the tree
        return ObjectStorageTypes.TREE.save(tree, repository);
    }
}
