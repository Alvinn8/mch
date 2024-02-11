package ca.bkaw.mch.fs;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * A wrapper around a {@link Path} used by {@link MchFileSystem mch file systems}.
 *
 * @see MchFileSystem
 */
public class MchPath implements Path {
    private final @NotNull MchFileSystem fileSystem;
    final @NotNull Path path;

    public MchPath(@NotNull MchFileSystem fileSystem, @NotNull Path path) {
        if (path instanceof MchPath) {
            throw new IllegalArgumentException("path must correspond to a non-mch file system.");
        }
        this.fileSystem = fileSystem;
        this.path = path;
    }

    /**
     * Wrap a {@link Path} in an {@link MchPath}.
     *
     * @param path The path.
     * @return The wrapped path.
     */
    @Contract("null -> null; !null -> !null")
    private @Nullable MchPath wrap(@Nullable Path path) {
        if (path == null) {
            return null;
        }
        return new MchPath(this.fileSystem, path);
    }

    /**
     * Unwrap an {@link MchPath} to a normal path. If the path was already a normal path, it
     * will be returned as-is.
     *
     * @param path The path, possibly an {@link MchPath}.
     * @return The non-mch path.
     */
    @Contract("!null -> !null; null -> null")
    private @Nullable Path unwrap(@Nullable Path path) {
        if (path == null) {
            return null;
        }
        if (path instanceof MchPath mchPath) {
            return mchPath.path;
        }
        // Not mch path, probably already unwrapped.
        return path;
    }

    @Override
    public @NotNull MchFileSystem getFileSystem() {
        return this.fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return this.path.isAbsolute();
    }

    @Override
    public Path getRoot() {
        return wrap(this.path.getRoot());
    }

    @Override
    public Path getFileName() {
        return this.path.getFileName();
    }

    @Override
    public Path getParent() {
        return wrap(this.path.getParent());
    }

    @Override
    public int getNameCount() {
        return this.path.getNameCount();
    }

    @Override
    public Path getName(int index) {
        return this.path.getName(index);
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return this.path.subpath(beginIndex, endIndex);
    }

    @Override
    public boolean startsWith(@NotNull Path other) {
        return this.path.startsWith(unwrap(other));
    }

    @Override
    public boolean endsWith(@NotNull Path other) {
        return this.path.endsWith(unwrap(other));
    }

    @Override
    public Path normalize() {
        return wrap(this.path.normalize());
    }

    @Override
    public Path resolve(@NotNull Path other) {
        return wrap(this.path.resolve(unwrap(other)));
    }
    @Override
    public Path relativize(@NotNull Path other) {
        return wrap(this.path.relativize(unwrap(other)));
    }

    @Override
    public URI toUri() {
        // TODO possibly unsafe
        System.out.println("Possible unsafe toUri call.");
        return this.path.toUri();
    }

    @Override
    public Path toAbsolutePath() {
        return wrap(this.path.toAbsolutePath());
    }

    @Override
    public Path toRealPath(LinkOption @NotNull ... options) throws IOException {
        return wrap(this.path.toRealPath(options));
    }

    @Override
    public WatchKey register(@NotNull WatchService watcher, WatchEvent.Kind<?> @NotNull [] events, WatchEvent.Modifier... modifiers) throws IOException {
        // TODO possibly unsafe
        System.out.println("Possible unsafe watch register call.");
        return this.path.register(watcher, events, modifiers);
    }

    @Override
    public int compareTo(@NotNull Path other) {
        return this.path.compareTo(unwrap(other));
    }

    @NotNull
    @Override
    public File toFile() {
        // TODO possibly unsafe.
        System.out.println("Possible unsafe toFile call.");
        Thread.dumpStack();
        // Known usages of File in game code:
        // - PlayerDataStorage (can quite easily be mixin'd)
        // - OldUsersConverter (can probably ignore)
        // - DimensionDataStorage
        // - Server icon (MinecraftServer)
        return this.path.toFile();
    }

    @Override
    public int hashCode() {
        return this.path.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MchPath mchPath)) {
            return false;
        }
        if (!this.fileSystem.equals(mchPath.fileSystem)) return false;
        return this.path.equals(mchPath.path);
    }

    @Override
    public String toString() {
        return this.path.toString();
    }
}
