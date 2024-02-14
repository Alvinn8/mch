package ca.bkaw.mch.fs;

import ca.bkaw.mch.chunk.RegionFileChunk;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.blob.Blob;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.region.MchRegionFile;
import ca.bkaw.mch.region.RegionStorageVisitor;
import ca.bkaw.mch.region.mc.McRegionFileWriter;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MchFileSystem extends FileSystem {
    private final MchFileSystemProvider provider;
    private final MchRepository repository;
    private TrackedWorld trackedWorld;
    private String dimensionKey;
    private Dimension dimension;
    private final Path root;
    private Set<Path> restoredPaths;

    public MchFileSystem(MchFileSystemProvider provider, Path root, MchRepository repository, TrackedWorld trackedWorld, String dimensionKey, Dimension dimension) {
        if (root.getFileSystem() instanceof MchFileSystem) {
            throw new IllegalArgumentException("root path must correspond to a non-mch file system.");
        }
        if (!root.isAbsolute()) {
            throw new IllegalArgumentException("root path must be absolute.");
        }
        this.provider = provider;
        this.root = root;
        this.repository = repository;
        this.trackedWorld = trackedWorld;
        this.dimensionKey = dimensionKey;
        this.dimension = dimension;
        this.restoredPaths = new HashSet<>();
    }

    /**
     * Set the {@link World} object that this file system reads from.
     *
     * @param trackedWorld The tracked world.
     * @param dimensionKey The dimension key.
     * @param dimension The dimension snapshot.
     */
    public void setWorld(TrackedWorld trackedWorld, String dimensionKey, Dimension dimension) {
        this.trackedWorld = trackedWorld;
        this.dimensionKey = dimensionKey;
        this.dimension = dimension;
        this.restoredPaths = new HashSet<>();
    }

    /**
     * Restore a file from the mch repository, so it becomes available on the real file
     * system.
     *
     * @param mchPath The mch path.
     */
    public void restore(@NotNull MchPath mchPath) throws IOException {
        // TODO Think about how to handle errors here. For example if a file does not
        //  exist, should this error or not do anything?
        if (mchPath.getFileSystem() != this) {
            throw new ProviderMismatchException();
        }
        if (this.restoredPaths.contains(mchPath.path)) {
            return;
        }
        System.out.println("DEBUG Restoring " + mchPath);
        Path relative = this.root.relativize(mchPath.path.toAbsolutePath());


        if ("region".equals(relative.getName(0).getFileName().toString())) {
            String fileName = relative.getFileName().toString();
            if ("region".equals(fileName)) {
                // lazy but works
                Files.createDirectories(mchPath.path);
                this.restoredPaths.add(mchPath.path);
                return;
            }
            System.out.println("fileName = " + fileName);
            String str = fileName.substring("r.".length(), fileName.length() - ".mca".length());
            String[] split = str.split("\\.");
            int regionX = Integer.parseInt(split[0]);
            int regionZ = Integer.parseInt(split[1]);
            Dimension.RegionFileReference regionFileRef = dimension.getRegionFile(regionX, regionZ);

            if (regionFileRef == null) {
                return;
            }

            Path regionStoragePath = RegionStorageVisitor.getPath(
                this.repository, this.trackedWorld, this.dimensionKey, regionX, regionZ
            );

            Path mchRegionFilePath = MchRegionFile.getPath(
                this.repository, this.trackedWorld, this.dimensionKey, regionX, regionZ
            );

            Path mcRegionFilePath = mchPath.path;
            try (McRegionFileWriter regionFile = new McRegionFileWriter(mcRegionFilePath)) {
                int[] chunkVersionNumbers = MchRegionFile.read(mchRegionFilePath, regionFileRef.getVersionNumber());

                RegionStorageVisitor.visitReadOnly(regionStoragePath, chunk -> {
                    int chunkVersionNumber = chunkVersionNumbers[chunk.getIndex()];
                    if (chunkVersionNumber != 0) {
                        RegionFileChunk restoredChunk = chunk.restore(chunkVersionNumber);
                        regionFile.writeChunk(restoredChunk.nbt(), restoredChunk.lastModified());
                    }
                });
            }
            this.restoredPaths.add(mchPath.path);
            return;
        }

        // Non-region file.
        Tree tree = dimension.getMiscellaneousFiles().resolve(this.repository);
        for (int i = 0; i < relative.getNameCount() - 1; i++) {
            String name = relative.getName(i).toString();
            Reference20<Tree> subTreeRef = tree.getSubTrees().get(name);
            if (subTreeRef == null) {
                return;
            }
            tree = subTreeRef.resolve(this.repository);
        }
        String fileName = relative.getFileName().toString();
        if (tree.getSubTrees().containsKey(fileName)) {
            Files.createDirectories(mchPath.path);
            System.out.println("Creating: " + mchPath.path);
        } else if (tree.getFiles().containsKey(fileName)) {
            Tree.BlobReference blobRef = tree.getFiles().get(fileName);
            Blob blob = blobRef.reference().resolve(this.repository);
            Files.write(mchPath.path, blob.getBytes());
        }
        this.restoredPaths.add(mchPath.path);
        // else nothing to restore, do nothing.
    }

    public DirectoryStream<Path> newDirectoryStream(@NotNull MchPath dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        // TODO respect filter
        if (dir.getFileSystem() != this) {
            throw new ProviderMismatchException();
        }
        System.out.println("DEBUG Listing " + dir);
        // Thread.dumpStack();
        Path relative = this.root.relativize(dir.path.toAbsolutePath().normalize());


        if ("region".equals(relative.getName(0).getFileName().toString())) {
            if (relative.getNameCount() != 1) {
                // No files in subdirectories of region.
                return new DirectoryStream<>() {
                    @Override
                    public Iterator<Path> iterator() {
                        return Collections.emptyIterator();
                    }
                    @Override
                    public void close() {}
                };
            }
            List<Path> list = dimension.getRegionFiles().stream().map((region) -> {
                String fileName = Util.formatRegionFileName(region.getRegionX(), region.getRegionZ(), ".mca");
                return dir.resolve(fileName);
            }).toList();

            return new DirectoryStream<>() {
                @Override
                public Iterator<Path> iterator() {
                    return list.iterator();
                }
                @Override
                public void close() {}
            };
        }

        // Non-region file.
        Tree tree = dimension.getMiscellaneousFiles().resolve(this.repository);
        for (int i = 0; i < relative.getNameCount(); i++) {
            String name = relative.getName(i).toString();
            if (name.isEmpty() || ".".equals(name)) {
                continue;
            }
            Reference20<Tree> subTreeRef = tree.getSubTrees().get(name);
            if (subTreeRef == null) {
                System.out.println("i = " + i + " : " + name);
                throw new NoSuchFileException(dir.toString());
            }
            tree = subTreeRef.resolve(this.repository);
        }

        List<Path> list = new ArrayList<>(tree.getFiles().size() + tree.getSubTrees().size());
        for (String name : tree.getFiles().keySet()) {
            list.add(dir.resolve(name));
        }
        for (String name : tree.getSubTrees().keySet()) {
            list.add(dir.resolve(name));
        }

        return new DirectoryStream<>() {
            @Override
            public Iterator<Path> iterator() {
                return list.iterator();
            }
            @Override
            public void close() {}
        };
    }

    @Override
    public Path getPath(@NotNull String first, String @NotNull ... more) {
        return new MchPath(this, this.root.getFileSystem().getPath(first, more));
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return List.of(new MchPath(this, this.root));
    }

    @Override
    public FileSystemProvider provider() {
        return this.provider;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return null;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return null;
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() {
        throw new UnsupportedOperationException();
    }
}
