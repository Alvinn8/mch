package ca.bkaw.mch.util;

/**
 * An abstract pathname that is not tied to a file system. Used to represent paths
 * relative to a world.
 * <p>
 * The forward slash {@code /} is always used as a directory separator regardless
 * of operating system.
 */
public class StringPath {
    private final String path;

    private StringPath(String path) {
        this.path = path;
    }

    /**
     * Create a {@link StringPath} from a {@link String} containing the path.
     *
     * @param path The {@link StringPath}.
     * @return The {@link StringPath}.
     */
    public static StringPath of(String path) {
        return new StringPath(path.replace('\\', '/'));
    }

    /**
     * A path that represents the root directory of the world.
     *
     * @return The {@link StringPath}.
     */
    public static StringPath root() {
        return new StringPath("/");
    }

    @Override
    public String toString() {
        return this.path;
    }

    /**
     * Resolve a path. If the relative path starts with a slash, the new path will
     * become the relative path.
     *
     * @param relative The relative path.
     * @return The resolved path.
     */
    public StringPath resolve(String relative) {
        if (relative.startsWith("/")) {
            return new StringPath(relative);
        }
        return new StringPath(Util.trailingSlash(this.path) + relative);
    }

    /**
     * Resolve a path, but force the resolution to be relative to the current path. In
     * other words, this method can never lead to the path "escaping" out to parent
     * directories of the current path.
     *
     * @param relative The relative path.
     * @return The resolved path.
     */
    public StringPath resolveRelative(String relative) {
        return new StringPath(Util.trailingSlash(this.path) + Util.noLeadingSlash(relative));
    }

    public String getFileName() {
        int index = this.path.lastIndexOf('/');
        return this.path.substring(index + 1);
    }

    /**
     * Get the name part of the path.
     *
     * @param index The index of the part.
     * @return The name.
     * @throws IllegalArgumentException If the index is negative or out of bounds.
     */
    public String getName(int index) {
        String[] parts = this.path.split("/");
        if (index < 0 || index >= parts.length) {
            throw new IllegalArgumentException(index + " is out of bounds.");
        }
        return parts[index];
    }

    public int getNameCount() {
        return this.path.split("/").length;
    }
}
