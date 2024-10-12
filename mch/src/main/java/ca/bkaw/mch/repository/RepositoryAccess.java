package ca.bkaw.mch.repository;

import ca.bkaw.mch.Sha1;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Provides read access to a repository.
 */
public interface RepositoryAccess {
	/**
	 * Access the dimension of a specific commit.
	 * <p>
	 * If the commit, world, or dimension were not found, {@code null} is returned.
	 *
	 * @param commitSha1 The SHA-1 hash of the commit.
	 * @param worldSha1 The SHA-1 hash of the world to restore the file from.
	 * @param dimensionKey The dimension key of the dimension of the world.
	 * @return The {@link DimensionAccess} object, or null.
	 * @throws IOException If an I/O error occurs.
	 */
	@Nullable
	DimensionAccess accessDimension(Sha1 commitSha1, Sha1 worldSha1, String dimensionKey) throws IOException;
}
