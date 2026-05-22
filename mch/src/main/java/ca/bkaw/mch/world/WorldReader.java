package ca.bkaw.mch.world;

import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.blob.Blob;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.util.StringPath;
import ca.bkaw.mch.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A class used to read high-level information from a world. Files and directories
 * are gotten from a {@link WorldProvider}.
 */
public class WorldReader {
    private final WorldProvider provider;

    public WorldReader(WorldProvider provider) {
        this.provider = provider;
    }

    @NotNull
    private FileInfo.Metadata metadata(FileInfo fileInfo) throws IOException {
        if (fileInfo.metadata() != null) {
            return fileInfo.metadata();
        }
        // Some providers do not include metadata when listing directories. We need
        // to stat to get the metadata. We know the file exists, so it's safe to
        // assume not null.
        return Objects.requireNonNull(
            this.provider.stat(fileInfo.path()),
            "File not found. Was the file deleted?"
        );
    }

    /**
     * Determine the world layout by checking for the presence of certain files and
     * directories.
     * <p>
     * May return null if the layout cannot be determined, which may indicate that the
     * folder is not a world.
     *
     * @return The world layout, or null.
     * @throws IOException If an I/O error occurs.
     */
    @Nullable
    public World.Layout determineWorldLayout() throws IOException {
        if (this.provider.stat(StringPath.root()) == null) {
            return null;
        }
        boolean classicLayout = false;
        boolean dimensionsLayout = false;
        for (FileInfo file : this.provider.list(StringPath.root())) {
            String name = file.name();
            if (isClassicLayoutDirectoryName(name)) {
                classicLayout = true;
                continue;
            }
            if ("dimensions".equals(name) && hasVanillaDimensionsLayout()) {
                dimensionsLayout = true;
            }
        }

        if (classicLayout && dimensionsLayout) {
            throw new RuntimeException("The world has both classic and new layout. This may cause unexpected behavior and is not supported by mch.");
        } else if (classicLayout) {
            return World.Layout.CLASSIC;
        } else if (dimensionsLayout) {
            return World.Layout.DIMENSIONS;
        } else {
            return null;
        }
    }

    private boolean isClassicLayoutDirectoryName(String name) {
        return "region".equals(name)
            || Util.NETHER_FOLDER.equals(name)
            || Util.THE_END_FOLDER.equals(name);
    }

    /**
     * Check for vanilla dimensions in the dimensions directory.
     *
     * @return true if the dimensions directory contains vanilla dimensions, false otherwise.
     * @throws IOException If an I/O error occurs.
     */
    private boolean hasVanillaDimensionsLayout() throws IOException {
        if (this.provider.stat(StringPath.of("/dimensions/minecraft")) == null) {
            return false;
        }

        for (FileInfo dimension : this.provider.list(StringPath.of("/dimensions/minecraft"))) {
            String name = dimension.name();
            if ("overworld".equals(name) || "the_nether".equals(name) || "the_end".equals(name)) {
                return true;
            }
        }

        return false;
    }

    public List<String> getClassicDimensions() throws IOException {
        StringPath rootPath = StringPath.root();
        List<FileInfo> directories = this.provider.list(rootPath);

        List<String> dimensions = new ArrayList<>(3);
        for (FileInfo fileInfo : directories) {
            switch (fileInfo.name()) {
                case "region" -> dimensions.add(Dimension.OVERWORLD);
                case Util.NETHER_FOLDER -> dimensions.add(Dimension.NETHER);
                case Util.THE_END_FOLDER -> dimensions.add(Dimension.THE_END);
                case "dimensions" -> {
                    StringPath dimensionsPath = rootPath.resolve("dimensions");
                    for (FileInfo namespaceDirectory : this.provider.list(dimensionsPath)) {
                        if (!metadata(namespaceDirectory).isDirectory()) {
                            continue;
                        }
                        String namespace = namespaceDirectory.name();
                        for (FileInfo keyDirectory : this.provider.list(dimensionsPath.resolve(namespace))) {
                            if (!metadata(keyDirectory).isDirectory()) {
                                continue;
                            }
                            String key = keyDirectory.name();
                            String dimensionKey = namespace + ':' + key;
                            dimensions.add(dimensionKey);
                        }
                    }
                }
            }
        }
        return dimensions;
    }

    public List<String> getDimensions(World.Layout layout) throws IOException {
        return switch (layout) {
            case CLASSIC -> getClassicDimensions();
            case DIMENSIONS -> {
                StringPath dimensionsPath = StringPath.of("dimensions");
                if (this.provider.stat(dimensionsPath) == null) {
                    yield List.of();
                }
                List<String> dimensions = new ArrayList<>();
                for (FileInfo namespaceDirectory : this.provider.list(dimensionsPath)) {
                    if (!metadata(namespaceDirectory).isDirectory()) {
                        continue;
                    }
                    String namespace = namespaceDirectory.name();
                    for (FileInfo keyDirectory : this.provider.list(dimensionsPath.resolve(namespace))) {
                        if (!metadata(keyDirectory).isDirectory()) {
                            continue;
                        }
                        String key = keyDirectory.name();
                        String dimensionKey = namespace + ':' + key;
                        dimensions.add(dimensionKey);
                    }
                }
                yield dimensions;
            }
        };
    }

    public List<RegionFileInfo> getRegionFiles(World.Layout layout, String dimension) throws IOException {
        StringPath path = Util.getDimensionPath(layout, dimension).resolve("region");
        if (this.provider.stat(path) == null) {
            return List.of();
        }
        List<RegionFileInfo> regionFiles = new ArrayList<>();

        for (FileInfo fileInfo : this.provider.list(path)) {
            String fileName = fileInfo.name();
            if (!fileName.startsWith("r.") || !fileName.endsWith(".mca")) {
                continue;
            }
            FileInfo.Metadata metadata = metadata(fileInfo);

            regionFiles.add(new RegionFileInfo(
                fileName, metadata.lastModified(), metadata.fileSize()
            ));
        }
        return regionFiles;
    }

    public RandomAccessReader openRegionFile(World.Layout layout, String dimension, String regionFileName, long estimatedSize) throws IOException {
        StringPath path = Util.getDimensionPath(layout, dimension).resolve("region").resolve(regionFileName);
        return this.provider.openFile(path, estimatedSize);
    }

    public Reference20<Tree> trackDirectoryTree(World.Layout layout, String dimension, MchRepository repository, Predicate<String> predicate, @Nullable Tree currentTree) throws IOException {
        return this.trackDirectoryTreePath(Util.getDimensionPath(layout, dimension), repository, predicate, currentTree);
    }

    public Reference20<Tree> trackDirectoryTreePath(StringPath path, MchRepository repository, Predicate<String> predicate, @Nullable Tree currentTree) throws IOException {
        Tree tree = new Tree();
        for (FileInfo file : this.provider.list(path)) {
            String name = file.name();
            if (!predicate.test(name)) {
                continue;
            }
            // TODO repository-wide "mchignore"
            if (name.contains(".sqlite")) {
                continue;
            }
            FileInfo.Metadata metadata = metadata(file);
            if (metadata.isDirectory()) {
                // Track subdirectories
                Reference20<Tree> currentSubTreeReference = currentTree != null ? currentTree.getSubTrees().get(name) : null;
                Tree currentSubTree = currentSubTreeReference != null ? currentSubTreeReference.resolve(repository) : null;
                Reference20<Tree> subDirectoryReference = trackDirectoryTreePath(file.path(), repository, str -> true, currentSubTree);
                tree.addSubTree(name, subDirectoryReference);
            } else if (metadata.isFile()) {
                // Track files
                Tree.BlobReference currentBlobReference = currentTree != null ? currentTree.getFiles().get(name) : null;
                long lastModified = metadata.lastModified();
                if (currentBlobReference == null || currentBlobReference.lastModified() != lastModified) {
                    // The file has changed since last commit. Save it anew.
                    byte[] bytes = this.provider.readFile(file.path(), metadata.fileSize());
                    Blob blob = new Blob(bytes);
                    Reference20<Blob> blobReference = ObjectStorageTypes.BLOB.save(blob, repository);
                    tree.addFile(name, new Tree.BlobReference(blobReference, lastModified));
                } else {
                    // The file has not changed since last commit. Reuse the reference.
                    tree.addFile(name, currentBlobReference);
                }
            }
        }

        // Save the tree
        return ObjectStorageTypes.TREE.save(tree, repository);
    }

}
