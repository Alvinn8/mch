package ca.bkaw.mch.world;

import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.util.StringPath;
import ca.bkaw.mch.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A world provider that reads from the a {@link Path}.
 * <p>
 * This is used when the world to track is on the same computer as the mch repository.
 * <p>
 * Instances of this class also act as the {@link WorldAccessor} instance since
 * there is no need to "connect" when accessing a world.
 */
public class DirectWorldProvider implements WorldAccessor, WorldProvider {
    public static final byte ID = 1;

    protected final Path path;

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
    public WorldProvider access(MchRepository mchRepository) {
        // Nothing needs to connect, so we reuse the same instance.
        return this;
    }

    @Override
    public void close() throws IOException {}

    @Override
    public byte getId() {
        return ID;
    }

    private Path getPath(StringPath path) {
        return this.path.resolve(Util.noLeadingSlash(path.toString()));
    }

    @Override
    public List<FileInfo> list(StringPath path) throws IOException {
        try (Stream<Path> stream = Files.list(this.getPath(path))) {
            return stream.map(filePath -> new FileInfo(
                filePath.getFileName().toString(),
                path.resolve(filePath.getFileName().toString()),
                null
            ))
            .toList();
        }
    }

    @Nullable
    @Override
    public FileInfo.Metadata stat(StringPath path) throws IOException {
        try {
            BasicFileAttributes attrs = Files.readAttributes(this.getPath(path), BasicFileAttributes.class);
            return new FileInfo.Metadata(
                attrs.isRegularFile(),
                attrs.isDirectory(),
                attrs.size(),
                attrs.lastModifiedTime().toMillis()
            );
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    @Override
    public RandomAccessReader openFile(StringPath path, long estimatedSize) throws IOException {
        return RandomAccessReader.of(this.getPath(path));
    }

    @Override
    public byte[] readFile(StringPath path, long estimatedSize) throws IOException {
        return Files.readAllBytes(this.getPath(path));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DirectWorldProvider that = (DirectWorldProvider) o;

        return Objects.equals(this.path, that.path);
    }

    @Override
    public int hashCode() {
        return this.path != null ? this.path.hashCode() : 0;
    }
}
