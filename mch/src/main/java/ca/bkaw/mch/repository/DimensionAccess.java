package ca.bkaw.mch.repository;

import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.util.StringPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Provides read access to a dimension of a specific commit. Can be used to read
 * files and list directories.
 */
public interface DimensionAccess {
	/**
	 * Restore a file.
	 *
	 * @param path The path of the file, relative to the dimension.
	 * @return An input stream of the file contents.
	 * @throws IOException If an I/O error occurs.
	 */
	@Nullable
	InputStream restoreFile(StringPath path) throws IOException;

	/**
	 * List files or directories inside a directory.
	 * <p>
	 * To list the root directory of the dimension, use {@link StringPath#root()}.
	 *
	 * @param path The path of the folder, relative to the world.
	 * @return A {@link Tree} object containing the directory.
	 * @throws IOException If an I/O error occurs.
	 */
	@NotNull
	List<String> list(StringPath path) throws IOException;
}
