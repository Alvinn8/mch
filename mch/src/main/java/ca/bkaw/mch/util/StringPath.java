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

	public StringPath resolve(String relative) {
		if (relative.startsWith("/")) {
			return new StringPath(relative);
		}
		return new StringPath(Util.trailingSlash(this.path) + relative);
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
		return parts[0];
	}

	public int getNameCount() {
		return this.path.split("/").length;
	}
}
