package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.repository.MchRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An object that caches the commit history of a repository and allows efficient
 * lookup of the previous and next commit.
 * <p>
 * This object will cache commit objects for the repository, and when a commit is
 * looked up that is not cached, it will be looked up and then cached.
 * <p>
 * This object must be initialized by first adding one commit to the cache.
 * Subsequent lookups must always be done relative to a commit that is cached.
 */
public class CachedCommits {
    private final MchRepository repository;
    private final List<CommitInfo> cachedCommits = new ArrayList<>();

    public CachedCommits(MchRepository repository) {
        this.repository = repository;
    }

    /**
     * Set up the cache with the initial commit. The subsequent lookup must be done
     * relative to this commit.
     * <p>
     * The commit must belong to the same repository this object watches.
     *
     * @param commit The commit.
     */
    public void setup(@NotNull CommitInfo commit) throws IOException {
        if (this.cachedCommits.size() != 0) {
            throw new IllegalStateException("May only call setup once.");
        }
        // Read the commit to ensure it exists in the repository. Throws
        // ObjectNotFoundException otherwise.
        ObjectStorageTypes.COMMIT.read(commit.hash(), this.repository);

        this.cachedCommits.add(commit);
    }

    /**
     * Get the previous commit to the specified commit. If the specified commit is the
     * first commit, {@code null} will be returned.
     * <p>
     * The {@code commit} must already be cached, but the previous commit might not. If
     * the previous commit is not cached, it will be resolved.
     *
     * @param commit The commit.
     * @return The previous commit, or null.
     * @throws IOException If an I/O error occurs while reading the previous commit.
     * @throws IllegalArgumentException If {@code commit} was not cached.
     */
    @Nullable
    public CommitInfo previousCommit(@NotNull CommitInfo commit) throws IOException {
        int index = this.cachedCommits.indexOf(commit);
        if (index == -1) {
            throw new IllegalArgumentException("Commit was not cached " + commit.hash());
        }
        if (index > 0) {
            return this.cachedCommits.get(index - 1);
        }
        Reference20<Commit> previousCommitHash = commit.commit().getPreviousCommit();
        if (previousCommitHash == null) {
            return null;
        }
        Commit previousCommit = previousCommitHash.resolve(this.repository);
        CommitInfo previous = new CommitInfo(previousCommit, previousCommitHash.getSha1());
        this.cachedCommits.add(index, previous);
        return previous;
    }

    /**
     * Get the commit that comes after the specified commit. If the specified commit is
     * the head commit, {@code null} will be returned since there is no next commit.
     *
     * @param commit The commit.
     * @return The next commit, or null.
     * @throws IOException If an I/O error occurs while reading finding the next commit.
     */
    @Nullable
    public CommitInfo nextCommit(@NotNull CommitInfo commit) throws IOException {
        int index = this.cachedCommits.indexOf(commit);
        if (index == -1) {
            throw new IllegalArgumentException("Commit was not cached " + commit.hash());
        }
        if (index < this.cachedCommits.size() - 1) {
            return this.cachedCommits.get(index + 1);
        }

        Reference20<Commit> headHash = this.repository.getHeadCommit();
        if (headHash == null) {
            throw new IllegalStateException("No head");
        }
        if (headHash.getSha1().equals(commit.hash())) {
            // Head commit has no next commit. It's the latest commit.
            return null;
        }
        List<CommitInfo> toAdd = new ArrayList<>();

        Commit headCommit = headHash.resolve(this.repository);
        CommitInfo next = new CommitInfo(headCommit, headHash.getSha1());
        while (true) {
            Reference20<Commit> prevHash = next.commit().getPreviousCommit();
            if (prevHash == null) {
                throw new IllegalStateException("Searched commit was not found in repository.");
            }
            if (prevHash.getSha1().equals(commit.hash())) {
                // We found the searched commit.
                // This means that the "next" variable holds the next commit.
                break;
            }
            Commit prev = prevHash.resolve(this.repository);
            next = new CommitInfo(prev, prevHash.getSha1());
            toAdd.add(next);
        }

        // Now that the loop exited, "next" is the commit we want to return.
        // The "toAdd" array contains the commits we had to look up to find
        // it. We want to cache these commits. Add them in reverse order since
        // "toAdd" stores the head commit first, we want the head commit last.
        for (int i = toAdd.size() - 1; i >= 0; i--) {
            this.cachedCommits.add(toAdd.get(i));
        }

        return next;
    }

    /**
     * Get whether the specified commit has a commit after it.
     *
     * @param commit The commit.
     * @return Whether a "next" commit exists.
     * @throws IOException If an I/O error occurs.
     */
    public boolean hasNext(@NotNull CommitInfo commit) throws IOException {
        Reference20<Commit> head = this.repository.getHeadCommit();
        if (head == null) {
            throw new IllegalArgumentException("Commit not in repository (empty repository).");
        }
        return !head.getSha1().equals(commit.hash());
    }

    /**
     * Check if the specified commit has a previous commit.
     *
     * @param commit The commit.
     * @return Whether a previous commit exists.
     */
    public boolean hasPrevious(@NotNull CommitInfo commit) {
        return commit.commit().getPreviousCommit() != null;
    }
}
