package ca.bkaw.mch.fs;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TestMain {
    public static void main(String[] args) throws IOException {
        Path a = Path.of("/Users/Alvin/Documents/Programmering/mch-viewer-fabric-test/run/world/advancements/idontexist");

        Files.newDirectoryStream(a).iterator().forEachRemaining((path) -> System.out.println(path));
    }
    public static void main2(String[] args) throws IOException {
        MchRepository repository = new MchRepository(Path.of("/Users/Alvin/Documents/mch/test2/mch"));
        repository.readConfiguration();
        TrackedWorld trackedWorld = repository.getConfiguration().getTrackedWorld(Sha1.fromString("40a9e3d33e815f6da8c0054ba895b13280fe8683"));

        Reference20<Commit> commitRef = new Reference20<>(ObjectStorageTypes.COMMIT, Sha1.fromString("280742e4698392d1475de4dbe407917f440933d2"));
        Commit commit = commitRef.resolve(repository);

        WorldContainer worldContainer = commit.getWorldContainer().resolve(repository);

        Reference20<World> worldRef = worldContainer.getWorld(trackedWorld.getId());
        World world = worldRef.resolve(repository);

        Path testPath = Path.of("run/mch-fs-test").toAbsolutePath();
        Files.createDirectories(testPath);
        if (Files.list(testPath).count() > 0) {
            System.err.println("Please empty " + testPath + " before running test.");
            return;
        }

        Map<String, Object> env = Map.of(
            MchFileSystemProvider.REPO_ENV_KEY, repository,
            MchFileSystemProvider.TRACKED_WORLD_ENV_KEY, trackedWorld,
            MchFileSystemProvider.WORLD_ENV_KEY, world
        );

        URI uri = URI.create("mch:" + testPath);

        FileSystem fileSystem = FileSystems.newFileSystem(uri, env);

        Path path = fileSystem.getPath(".");

        System.out.println("Result:");
        System.out.println(Files.list(path).map(p -> p.toString()).toList());
    }
}
