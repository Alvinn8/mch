package ca.bkaw.mch.e2e;

import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.repository.MchRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static ca.bkaw.mch.e2e.EndToEndTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class LosslessLossyTest {
    @Test
    @Disabled("By default mch is currently lossy and has no way to change it. " +
        "Once a config option is added this test can be enabled again.")
    void lossless(@TempDir Path tempDir) throws IOException {
        Path worldPath = tempDir.resolve("world");
        extractWorld("world1", worldPath);
        mchCli(tempDir, "init");
        mchCli(tempDir, "world", "add", "local", worldPath.toAbsolutePath().toString());

        mchCli(tempDir, "commit");

        MchRepository repo = new MchRepository(tempDir.resolve("mch"));
        Reference20<Commit> headCommit = repo.getHeadCommit();
        assertNotNull(headCommit);

        mchCli(tempDir, "restore", headCommit.getSha1().asHex());

        Path restorePath = tempDir.resolve("mch-restore/world");
        assertTrue(Files.isDirectory(restorePath));

        assertWorldsEqualLossless(worldPath, restorePath);
    }

    @Test
    void lossy(@TempDir Path tempDir) throws IOException {
        Path worldPath = tempDir.resolve("world");
        extractWorld("world1", worldPath);
        mchCli(tempDir, "init");
        mchCli(tempDir, "world", "add", "local", worldPath.toAbsolutePath().toString());

        mchCli(tempDir, "commit");

        MchRepository repo = new MchRepository(tempDir.resolve("mch"));
        Reference20<Commit> headCommit = repo.getHeadCommit();
        assertNotNull(headCommit);

        mchCli(tempDir, "restore", headCommit.getSha1().asHex());

        Path restorePath = tempDir.resolve("mch-restore/world");
        assertTrue(Files.isDirectory(restorePath));

        assertWorldsEqualLossy(worldPath, restorePath, 0);
    }
}
