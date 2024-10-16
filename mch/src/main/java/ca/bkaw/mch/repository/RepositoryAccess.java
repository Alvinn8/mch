package ca.bkaw.mch.repository;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Provides read access to a repository.
 */
public interface RepositoryAccess {

	/**
	 * Get the head commit, the latest commit.
	 *
	 * @return The head commit.
	 * @throws IOException If an I/O error occurs.
	 */
	@Nullable
	Reference20<Commit> getHeadCommit() throws IOException;

	/**
	 * Get a list of the ids of the worlds tracked by this repository.
	 *
	 * @return The id.
	 */
	List<Sha1> getTrackedWorlds() throws IOException;

	/**
	 * Get the id of a tracked world by world name. If it does not exist, {@code null}
	 * is returned.
	 *
	 * @param name The name of the world.
	 * @return The SHA-1 id of the tracked world, or null.
	 */
	@Nullable
	Sha1 getTrackedWorld(String name) throws IOException;

	/**
	 * Get the dimensions that exist for a particular commit in the specified world.
	 *
	 * @param worldSha1 The SHA-1 of the tracked world.
	 * @return The list of dimension keys.
	 * @throws IOException If an I/O error occurs.
	 */
	List<String> getDimensions(Sha1 commitSha1, Sha1 worldSha1) throws IOException;

	/**
	 * Get the {@link Commit} object by the SHA-1 hash of the commit.
	 *
	 * @param commitSha1 The SHA-1 hash of the commit.
	 * @return The commit object.
	 * @throws IOException If an I/O error occurs.
	 */
	Commit accessCommit(Sha1 commitSha1) throws IOException;

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
