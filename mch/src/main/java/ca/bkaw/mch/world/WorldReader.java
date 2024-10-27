package ca.bkaw.mch.world;

import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.blob.Blob;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.util.StringPath;
import ca.bkaw.mch.util.Util;
import ca.bkaw.mch.world.sftp.OutputStreamFileDest;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class WorldReader {
    private final WorldProvider provider;

    public WorldReader(WorldProvider provider) {
        this.provider = provider;
    }

    @NotNull
    private FileInfo.Metadata metadata(FileInfo fileInfo) {
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

    public List<String> getDimensions() {
        List<FileInfo> directories = this.provider.list(StringPath.root());

        List<String> dimensions = new ArrayList<>(3);
        for (FileInfo fileInfo : directories) {
            String dimension = switch (fileInfo.name()) {
                case "region" -> Dimension.OVERWORLD;
                case Util.NETHER_FOLDER -> Dimension.NETHER;
                case Util.THE_END_FOLDER -> Dimension.THE_END;
                default -> null;
            };
            if (dimension != null && metadata(fileInfo).isDirectory()) {
                dimensions.add(dimension);
            }
        }
        // TODO custom dimensions
        return dimensions;
    }

    private StringPath getDimensionPath(String dimension) {
        return switch (dimension) {
            case Dimension.OVERWORLD -> StringPath.root();
            case Dimension.NETHER -> StringPath.of(Util.NETHER_FOLDER);
            case Dimension.THE_END -> StringPath.of(Util.THE_END_FOLDER);
            default -> StringPath.of( "dimensions/" + dimension.replace(':', '/'));
        };
    }

    public List<RegionFileInfo> getRegionFiles(String dimension) throws IOException {
        StringPath path = getDimensionPath(dimension).resolve("region");
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

    public RandomAccessReader openRegionFile(String dimension, String regionFileName, long estimatedSize) throws IOException {
        StringPath path = this.getDimensionPath(dimension).resolve("region").resolve(regionFileName);
        return this.provider.openFile(path, estimatedSize);
    }

    public Reference20<Tree> trackDirectoryTree(String dimension, MchRepository repository, Predicate<String> predicate, @Nullable Tree currentTree) throws IOException {
        return this.trackDirectoryTreePath(this.getDimensionPath(dimension), repository, predicate, currentTree);
    }

    public Reference20<Tree> trackDirectoryTreePath(StringPath path, MchRepository repository, Predicate<String> predicate, @Nullable Tree currentTree) throws IOException {
        Tree tree = new Tree();
        for (FileInfo file : this.provider.list(path)) {
            String name = file.name();
            if (!predicate.test(name)) {
                continue;
            }
            // TODO repository-wide "mchignore"
            if (name.contains("ledger.sqlite")) {
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
