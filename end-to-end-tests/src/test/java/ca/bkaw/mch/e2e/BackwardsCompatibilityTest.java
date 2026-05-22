package ca.bkaw.mch.e2e;

import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.repository.MchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static ca.bkaw.mch.e2e.EndToEndTestUtils.assertWorldsEqualLossy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BackwardsCompatibilityTest {
	@Test
	void vn13(@TempDir Path tempDir) throws IOException {
		performBackwardsCompatibilityTest("vn13", tempDir);
	}

	void performBackwardsCompatibilityTest(String repoName, Path tempDir) throws IOException {
		Path worldPath = tempDir.resolve("world");
		EndToEndTestUtils.extractRepo(repoName, tempDir);

		MchRepository repo = MchRepository.of(tempDir);
		repo.readConfiguration();
		assertTrue(repo.exists(), "Repository should exist after extracting test repo");

		testRestore(tempDir, worldPath, repo);
		testCommit(tempDir, worldPath, repo);
	}

	// Test that mch is able to restore a commit from an older version of mch.
	void testRestore(Path tempDir, Path worldPath, MchRepository repo) throws IOException {
		Reference20<Commit> headCommit = repo.getHeadCommit();
		assertNotNull(headCommit);

		EndToEndTestUtils.mchCli(tempDir, "restore", headCommit.getSha1().asHex());
		Path restorePath = tempDir.resolve("mch-restore/world");
		assertTrue(Files.isDirectory(restorePath));

		assertWorldsEqualLossy(worldPath, restorePath, 0);

		// Rename it so that it doesn't interfere with other tests.
		Files.move(tempDir.resolve("mch-restore"), tempDir.resolve("mch-restore-testRestore"));
	}

	// Test that mch is able to commit to the repository. This will update some objects
	// in the repository to the new format. Ensure that the world can still be restored
	// after this.
	//
	// Also test that the previous commit can be restored. This should always work,
	// unless mutable objects have been changed in a way that breaks old commits.
	// This test aims to catch such breaking changes.
	void testCommit(Path tempDir, Path worldPath, MchRepository repo) throws IOException {
		Reference20<Commit> oldCommit = repo.getHeadCommit();
		assertNotNull(oldCommit);

		EndToEndTestUtils.mchCli(tempDir, "commit");

		Reference20<Commit> headCommit = repo.getHeadCommit();
		assertNotNull(headCommit);

		// Restore old commit
		EndToEndTestUtils.mchCli(tempDir, "restore", oldCommit.getSha1().asHex());
		Path restorePath = tempDir.resolve("mch-restore/world");
		assertTrue(Files.isDirectory(restorePath));

		assertWorldsEqualLossy(worldPath, restorePath, 0);

		// Rename it so that it doesn't interfere with other tests.
		Files.move(tempDir.resolve("mch-restore"), tempDir.resolve("mch-restore-old-testCommit"));

		// Restore new commit

		EndToEndTestUtils.mchCli(tempDir, "restore", headCommit.getSha1().asHex());
		assertTrue(Files.isDirectory(restorePath));

		assertWorldsEqualLossy(worldPath, restorePath, 0);

		// Rename it so that it doesn't interfere with other tests.
		Files.move(tempDir.resolve("mch-restore"), tempDir.resolve("mch-restore-new-testCommit"));
	}
}
