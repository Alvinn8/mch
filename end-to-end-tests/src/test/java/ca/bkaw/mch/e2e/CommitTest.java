package ca.bkaw.mch.e2e;

import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import ca.bkaw.mch.repository.MchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static ca.bkaw.mch.e2e.EndToEndTestUtils.extractWorld;
import static ca.bkaw.mch.e2e.EndToEndTestUtils.mchCli;
import static org.junit.jupiter.api.Assertions.*;

public class CommitTest {
    @Test
    void testCreateAndCommitEmptyRepository(@TempDir Path tempDir) throws IOException {
        extractWorld("world1", tempDir.resolve("world"));
        mchCli(tempDir, "init");
        mchCli(tempDir, "world", "add", "local", tempDir.resolve("world").toAbsolutePath().toString());

        mchCli(tempDir, "commit");

        // Check that there is exactly one commit.
        MchRepository repo = new MchRepository(tempDir.resolve("mch"));
        Reference20<Commit> commitRef = repo.getHeadCommit();
        assertNotNull(commitRef);

        // There should be no previous commit.
        Commit commit = commitRef.resolve(repo);
        assertNull(commit.getPreviousCommit());

        WorldContainer worldContainer = commit.getWorldContainer().resolve(repo);
        assertEquals(1, worldContainer.getWorlds().size());
        World world = worldContainer.getWorlds().values().iterator().next().resolve(repo);

        // The overworld should be tracked with exactly 4 region files
        Reference20<Dimension> dimensionRef = world.getDimension(Dimension.OVERWORLD);
        assertNotNull(dimensionRef);
        Dimension dimension = dimensionRef.resolve(repo);
        assertEquals(4, dimension.getRegionFiles().size());

        // There should be a level.dat file
        Tree miscFiles = dimension.getMiscellaneousFiles().resolve(repo);
        assertTrue(miscFiles.getFiles().containsKey("level.dat"));
    }

    @Test
    void twoCommitsSameWorldHasSameWorldContainerHash(@TempDir Path tempDir) throws IOException {
        extractWorld("world1", tempDir.resolve("world"));
        mchCli(tempDir, "init");
        mchCli(tempDir, "world", "add", "local", tempDir.resolve("world").toAbsolutePath().toString());

        mchCli(tempDir, "commit");
        mchCli(tempDir, "commit");

        MchRepository repo = new MchRepository(tempDir.resolve("mch"));
        Reference20<Commit> headCommitRef = repo.getHeadCommit();
        assertNotNull(headCommitRef);
        Commit headCommit = headCommitRef.resolve(repo);

        Reference20<Commit> previousCommitRef = headCommit.getPreviousCommit();
        assertNotNull(previousCommitRef);
        Commit previousCommit = previousCommitRef.resolve(repo);

        assertEquals(
            headCommit.getWorldContainer().getSha1(),
            previousCommit.getWorldContainer().getSha1(),
            "Expected two commits on the same world to get the same world container hash."
        );
    }
}
