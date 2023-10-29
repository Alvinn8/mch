package ca.bkaw.mch.cli.world;

import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.world.WorldAccessor;
import ca.bkaw.mch.world.zip.ZipWorldAccessor;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;

@Command(name = "edit")
public class EditWorldCommand {
    @Inject
    MchRepository repository;

    @Command(name = "zip")
    public int zip(
        @Parameters(index = "0", paramLabel = "world name or id")
        String identifier,
        @Parameters(index = "1", paramLabel = "zip file")
        Path zipPath,
        @Parameters(index = "2", paramLabel = "path of world inside zip", defaultValue = "/")
        String pathInsideZip
    ) throws IOException {
        MchConfiguration configuration = this.repository.getConfiguration();

        TrackedWorld world = null;
        for (TrackedWorld trackedWorld : configuration.getTrackedWorlds()) {
            if (identifier.equals(trackedWorld.getName())
                || identifier.equals(trackedWorld.getId().asHex())) {
                world = trackedWorld;
                break;
            }
        }
        if (world == null) {
            System.err.println("World not found");
            return ExitCode.USAGE;
        }

        zipPath = zipPath.toAbsolutePath().normalize();
        WorldAccessor worldAccessor = new ZipWorldAccessor(zipPath, pathInsideZip);

        world.setWorldAccessor(worldAccessor);

        this.repository.saveConfiguration();

        System.out.println("Changed the world accessor.");
        return ExitCode.OK;
    }
}
