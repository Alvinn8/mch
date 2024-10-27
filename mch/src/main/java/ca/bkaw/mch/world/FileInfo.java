package ca.bkaw.mch.world;

import ca.bkaw.mch.util.StringPath;
import org.jetbrains.annotations.Nullable;

/**
 * A file name with optional file metadata.
 *
 * @param name The name of the file or directory.
 * @param path The path to the file relative to the provider's root.
 * @param metadata Metadata about the file or directory.
 */
public record FileInfo(String name, StringPath path, @Nullable Metadata metadata) {
    /**
     * Metadata about a file or directory.
     *
     * @param isFile {@code true} if a regular file.
     * @param isDirectory {@code true} if a directory.
     * @param fileSize The file size of the file. May be undefined for directories.
     * @param lastModified The last modification time of the file, measured in epoch milliseconds.
     */
    public record Metadata(boolean isFile, boolean isDirectory, long fileSize, long lastModified) {
    }
}
