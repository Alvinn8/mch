package ca.bkaw.mch.world.ftp;

import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.util.Util;
import ca.bkaw.mch.world.RegionFileInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FtpWorldProviderTest {
    static FakeFtpServer ftpServer;
    static FtpWorldProvider worldProvider;

    @BeforeAll
    static void setup() throws IOException {
        ftpServer = new FakeFtpServer();
        ftpServer.addUserAccount(new UserAccount("username", "password", "/root"));

        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/root"));
        fileSystem.add(new DirectoryEntry("/root/world"));
        fileSystem.add(new FileEntry("/root/world/level.dat"));
        fileSystem.add(new DirectoryEntry("/root/world/region"));
        fileSystem.add(new FileEntry("/root/world/region/r.0.0.mca", "a"));
        fileSystem.add(new FileEntry("/root/world/region/r.1.0.mca"));
        fileSystem.add(new FileEntry("/root/world/region/test.txt"));
        fileSystem.add(new DirectoryEntry("/root/world/" + Util.NETHER_FOLDER));
        ftpServer.setFileSystem(fileSystem);

        ftpServer.setServerControlPort(0); // any port
        ftpServer.start();

        worldProvider = new FtpWorldProvider(
            new FtpProfile(
                "localhost", ftpServer.getServerControlPort(), false,
                "username", "password"
            ),
            "world"
        );
    }

    @Test
    void dimensions() throws IOException {
        assertEquals(
            Set.of(Dimension.OVERWORLD, Dimension.NETHER),
            Set.copyOf(worldProvider.getDimensions())
        );
    }

    @Test
    void regionFiles() throws IOException {
        assertEquals(
            List.of("r.0.0.mca", "r.1.0.mca"),
            worldProvider.getRegionFiles(Dimension.OVERWORLD)
                .stream()
                .map(RegionFileInfo::fileName)
                .sorted()
                .toList()
        );
    }

    @Test
    void readRegionFile() throws IOException {
        try (RandomAccessReader reader = worldProvider.openRegionFile(Dimension.OVERWORLD, "r.0.0.mca")) {
            byte b = reader.readByte();
            assertEquals('a', b);
        }
    }
}
