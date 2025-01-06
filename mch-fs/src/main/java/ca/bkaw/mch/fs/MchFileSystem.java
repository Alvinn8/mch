package ca.bkaw.mch.fs;

import ca.bkaw.mch.repository.DimensionAccess;
import ca.bkaw.mch.util.StringPath;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class MchFileSystem extends FileSystem {
    private final MchFileSystemProvider provider;
    private DimensionAccess dimensionAccess;
    private final Path root;
    private Set<Path> restoredPaths;

    public MchFileSystem(MchFileSystemProvider provider, Path root, DimensionAccess dimensionAccess) {
        if (root.getFileSystem() instanceof MchFileSystem) {
            throw new IllegalArgumentException("root path must correspond to a non-mch file system.");
        }
        if (!root.isAbsolute()) {
            throw new IllegalArgumentException("root path must be absolute.");
        }
        this.provider = provider;
        this.root = root;
        this.dimensionAccess = dimensionAccess;
        this.restoredPaths = new HashSet<>();
    }

    /**
     * Set the {@link DimensionAccess} that this file system reads from.
     *
     * @param dimensionAccess The dimension access object.
     */
    public void setDimensionAccess(DimensionAccess dimensionAccess) throws IOException {
        this.dimensionAccess = dimensionAccess;
        for (Path path : this.restoredPaths) {
            try {
                Files.delete(path);
            } catch (DirectoryNotEmptyException ignored) {
                // Folders can stay. This is one IO call as supposed to first
                // checking if it is a folder, then deleting.
            }
        }
        this.restoredPaths = new HashSet<>();
    }

    /**
     * Restore a file from the mch repository, so it becomes available on the real file
     * system.
     *
     * @param mchPath The mch path.
     */
    public void restore(@NotNull MchPath mchPath) throws IOException {
        if (mchPath.getFileSystem() != this) {
            throw new ProviderMismatchException();
        }
        if (this.restoredPaths.contains(mchPath.path)) {
            return;
        }
        // TODO folders??
        // System.out.println("DEBUG Restoring " + mchPath);
        Path relative = this.root.relativize(mchPath.path.toAbsolutePath());
        Files.createDirectories(mchPath.path.getParent());
        try (InputStream stream = this.dimensionAccess.restoreFile(StringPath.of(relative.toString()))) {
            if (stream != null) {
                Files.copy(stream, mchPath.path, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        this.restoredPaths.add(mchPath.path);
    }

    public DirectoryStream<Path> newDirectoryStream(@NotNull MchPath dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        // TODO respect filter
        if (dir.getFileSystem() != this) {
            throw new ProviderMismatchException();
        }
        System.out.println("DEBUG Listing " + dir);
        // Thread.dumpStack();
        Path relative = this.root.relativize(dir.path.toAbsolutePath().normalize());
        List<String> fileNames = this.dimensionAccess.list(StringPath.of(relative.toString()));
        Stream<Path> files = fileNames.stream().map(dir::resolve);
        return new DirectoryStream<>() {
            @Override
            @NotNull
            public Iterator<Path> iterator() {
                return files.iterator();
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
