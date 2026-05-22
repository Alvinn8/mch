package ca.bkaw.mch.world.ftp;

import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.world.RegionFileInfo;
import ca.bkaw.mch.world.WorldProvider;
import ca.bkaw.mch.world.WorldReader;
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

public class WorldReaderDimensionsLayoutFtpTest {
	static FakeFtpServer ftpServer;
	static WorldReader worldReader;

	@BeforeAll
	static void setup() throws IOException {
		ftpServer = new FakeFtpServer();
		ftpServer.addUserAccount(new UserAccount("username", "password", "/root"));

		FileSystem fileSystem = new UnixFakeFileSystem();
		fileSystem.add(new DirectoryEntry("/root"));
		fileSystem.add(new DirectoryEntry("/root/world"));
		fileSystem.add(new FileEntry("/root/world/level.dat"));
		fileSystem.add(new DirectoryEntry("/root/world/dimensions/minecraft/overworld/region"));
		fileSystem.add(new FileEntry("/root/world/dimensions/minecraft/overworld/region/r.0.0.mca", "a"));
		fileSystem.add(new FileEntry("/root/world/dimensions/minecraft/overworld/region/r.1.0.mca"));
		fileSystem.add(new FileEntry("/root/world/dimensions/minecraft/overworld/region/test.txt"));
		fileSystem.add(new DirectoryEntry("/root/world/dimensions/minecraft/the_nether"));
		ftpServer.setFileSystem(fileSystem);

		ftpServer.setServerControlPort(0); // any port
		ftpServer.start();

		WorldProvider worldProvider = new FtpWorldProvider(
			new FtpProfile(
				"localhost", ftpServer.getServerControlPort(), false,
				"username", "password"
			),
			"world"
		);
		worldReader = new WorldReader(worldProvider);
	}

	@Test
	void dimensions() throws IOException {
		assertEquals(
			Set.of(Dimension.OVERWORLD, Dimension.NETHER),
			Set.copyOf(worldReader.getDimensions(World.Layout.DIMENSIONS))
		);
	}

	@Test
	void regionFiles() throws IOException {
		assertEquals(
			List.of("r.0.0.mca", "r.1.0.mca"),
			worldReader.getRegionFiles(World.Layout.DIMENSIONS, Dimension.OVERWORLD)
				.stream()
				.map(RegionFileInfo::fileName)
				.sorted()
				.toList()
		);
	}

	@Test
	void readRegionFile() throws IOException {
		try (RandomAccessReader reader = worldReader.openRegionFile(World.Layout.DIMENSIONS, Dimension.OVERWORLD, "r.0.0.mca", 32)) {
			byte b = reader.readByte();
			assertEquals('a', b);
		}
	}
}
