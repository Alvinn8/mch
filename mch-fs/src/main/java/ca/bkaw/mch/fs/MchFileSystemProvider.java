package ca.bkaw.mch.fs;

import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MchFileSystemProvider extends FileSystemProvider {
    public static final MchFileSystemProvider INSTANCE = new MchFileSystemProvider();

    public static final String SCHEME = "mch";
    public static final String REPO_ENV_KEY = "mch_repository";
    public static final String WORLD_ENV_KEY = "mch_world";
    public static final String TRACKED_WORLD_ENV_KEY = "mch_tracked_world";

    final Map<String, MchFileSystem> fileSystems = new HashMap<>();

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        synchronized (this.fileSystems) {
            if (!(env.get(REPO_ENV_KEY) instanceof MchRepository repository)) {
                throw new IllegalArgumentException("Please provide the mch repository as the \"" + REPO_ENV_KEY + "\" env key.");
            }
            if (!(env.get(WORLD_ENV_KEY) instanceof World world)) {
                throw new IllegalArgumentException("Please provide the mch world object as the \"" + WORLD_ENV_KEY + "\" env key.");
            }
            if (!(env.get(TRACKED_WORLD_ENV_KEY) instanceof TrackedWorld trackedWorld)) {
                throw new IllegalArgumentException("Please provide the tracked world as the \"" + TRACKED_WORLD_ENV_KEY + "\" env key.");
            }
            if (!SCHEME.equals(uri.getScheme())) {
                throw new IllegalArgumentException("Expected \"" + SCHEME + "\" as the URI scheme.");
            }
            String path = uri.getPath();
            Path root = Path.of(path);
            if (!Files.isDirectory(root)) {
                throw new IllegalArgumentException("Please make sure the provided path is a directory: " + root);
            }

            MchFileSystem fileSystem = this.fileSystems.get(path);
            if (fileSystem != null) {
                throw new FileSystemAlreadyExistsException(path);
            }
            fileSystem = new MchFileSystem(this, root, repository, trackedWorld, world);
            this.fileSystems.put(path, fileSystem);
            return fileSystem;
        }
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        return this.fileSystems.get(uri.getPath());
    }

    @Override
    public Path getPath(@NotNull URI uri) {
        throw new UnsupportedOperationException();
    }

    private @NotNull MchPath mchPath(@Nullable Path path) {
        if (!(path instanceof MchPath mchPath)) {
            throw new ProviderMismatchException();
        }
        return mchPath;
    }

    private void restore(@NotNull MchPath mchPath) throws IOException {
        MchFileSystem fileSystem = mchPath.getFileSystem();
        fileSystem.restore(mchPath);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        MchPath mchPath = this.mchPath(path);
        this.restore(mchPath);
        return Files.newByteChannel(mchPath.path, options, attrs);
    }

    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        MchPath mchPath = this.mchPath(path);
        this.restore(mchPath);
        return FileChannel.open(mchPath.path, options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        MchPath mchPath = this.mchPath(dir);
        return mchPath.getFileSystem().newDirectoryStream(mchPath, filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        MchPath mchPath = this.mchPath(dir);
        Files.createDirectory(mchPath.path, attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        MchPath mchPath = this.mchPath(path);
        Files.delete(mchPath.path);
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        MchPath mchSource = this.mchPath(source);
        this.restore(mchSource);
        MchPath mchTarget = this.mchPath(target);
        Files.copy(mchSource.path, mchTarget.path, options);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        MchPath mchSource = this.mchPath(source);
        this.restore(mchSource);
        MchPath mchTarget = this.mchPath(target);
        Files.move(mchSource.path, mchTarget.path, options);
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        // TODO maybe unsafe
        MchPath mchPath = this.mchPath(path);
        MchPath mchPath2 = this.mchPath(path2);
        return Files.isSameFile(mchPath.path, mchPath2.path);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        MchPath mchPath = this.mchPath(path);
        return Files.isHidden(mchPath);
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        MchPath mchPath = this.mchPath(path);
        this.restore(mchPath);
        mchPath.path.getFileSystem().provider().checkAccess(mchPath.path, modes);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        MchPath mchPath = this.mchPath(path);
        try {
            this.restore(mchPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Files.getFileAttributeView(mchPath.path, type, options);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        MchPath mchPath = this.mchPath(path);
        this.restore(mchPath);
        return Files.readAttributes(mchPath.path, type, options);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        MchPath mchPath = this.mchPath(path);
        this.restore(mchPath);
        return Files.readAttributes(mchPath.path, attributes, options);
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        MchPath mchPath = this.mchPath(path);
        this.restore(mchPath);
        Files.setAttribute(mchPath.path, attribute, value, options);
    }
}
