package ca.bkaw.mch.world;

import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.util.StringPath;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * An object that provides mch with file access to world.
 * <p>
 * When this object is created through a {@link WorldAccessor} it can be considered
 * as a session, and once access to the world is no longer necessary the world
 * provider object should be closed.
 */
public interface WorldProvider extends AutoCloseable {

    /**
     * List the files in a directory.
     * <p>
     * Some implementations are able to supply file metadata (such as file size and last
     * modification time) while listing, while other implementation will have metadata be
     * {@code null}.
     *
     * @param path The path of the directory to list.
     * @return The list of files.
     * @throws IOException If an I/O error occurs.
     */
    List<FileInfo> list(StringPath path) throws IOException;

    /**
     * Get metadata about a file or folder, if it exists. If there is no file or folder
     * at the path, {@code null} is returned.
     *
     * @param path The path to stat.
     * @return The metadata for the file or folder, or {@code null}.
     * @throws IOException If an I/O error occurs.
     */
    @Nullable
    FileInfo.Metadata stat(StringPath path) throws IOException;

    /**
     * Open a file for random access.
     *
     * @param path The path to the file
     * @param estimatedSize The estimated size of the file. Used for performance reasons.
     * @return A {@link RandomAccessReader} with the file contents.
     * @throws IOException If an I/O error occurs.
     */
    RandomAccessReader openFile(StringPath path, long estimatedSize) throws IOException;

    /**
     * Read a file fully.
     *
     * @param path The path to the file.
     * @param estimatedSize The estimated size of the file. Used for performance reasons.
     * @return The file content.
     * @throws IOException If an I/O error occurs.
     */
    byte[] readFile(StringPath path, long estimatedSize) throws IOException;

    /**
     * Close the world provider. Depending on the implementation, this method will
     * disconnect from file servers, etc.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Override
    void close() throws IOException;
}
