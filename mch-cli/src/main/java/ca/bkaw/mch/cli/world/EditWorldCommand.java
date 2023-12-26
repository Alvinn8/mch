package ca.bkaw.mch.cli.world;

import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.world.WorldAccessor;
import ca.bkaw.mch.world.sftp.SftpProfile;
import ca.bkaw.mch.world.sftp.SftpWorldAccessor;
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

    private TrackedWorld getWorld(String identifier) {
        MchConfiguration configuration = this.repository.getConfiguration();

        for (TrackedWorld trackedWorld : configuration.getTrackedWorlds()) {
            if (identifier.equals(trackedWorld.getName())
                || identifier.equals(trackedWorld.getId().asHex())) {
                return trackedWorld;
            }
        }
        return null;
    }

    @Command(name = "zip")
    public int zip(
        @Parameters(index = "0", paramLabel = "world name or id")
        String identifier,
        @Parameters(index = "1", paramLabel = "zip file")
        Path zipPath,
        @Parameters(index = "2", paramLabel = "path of world inside zip", defaultValue = "/")
        String pathInsideZip
    ) throws IOException {
        TrackedWorld world = this.getWorld(identifier);
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

    @Command(name = "sftp")
    public int sftp(
        @Parameters(index = "0", paramLabel = "world name or id")
        String identifier,
        @Parameters(index = "1")
        String sftpProfileName,
        @Parameters(index = "2", description = "The path of the world on the remote server.")
        String remotePath
    ) throws IOException {
        TrackedWorld world = this.getWorld(identifier);
        if (world == null) {
            System.err.println("World not found");
            return ExitCode.USAGE;
        }

        MchConfiguration configuration = this.repository.getConfiguration();
        SftpProfile sftpProfile = configuration.getSftpProfiles().getProfile(sftpProfileName);
        if (sftpProfile == null) {
            System.err.println("No SFTP profile with the name \"" + sftpProfileName + "\" exists.");
            System.err.println("Add it using \"mch sftp add " + sftpProfileName + "\"");
            return ExitCode.USAGE;
        }

        SftpWorldAccessor worldAccessor = new SftpWorldAccessor(sftpProfileName, remotePath);
        world.setWorldAccessor(worldAccessor);

        this.repository.saveConfiguration();

        System.out.println("Changed the world accessor.");
        return ExitCode.OK;
    }
}
