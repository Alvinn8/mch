package ca.bkaw.mch.cli.world;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.world.DirectWorldProvider;
import ca.bkaw.mch.world.WorldAccessor;
import ca.bkaw.mch.world.ftp.FtpProfile;
import ca.bkaw.mch.world.ftp.FtpWorldAccessor;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;

@Command(name = "add")
public class AddWorldCommand {
    @Inject
    MchRepository repository;

    private int track(WorldAccessor worldAccessor, String name) throws IOException {
        MchConfiguration configuration = repository.getConfiguration();

        if (configuration.alreadyTracking(worldAccessor)) {
            System.err.println("That world is already being tracked my this repository.");
            return ExitCode.USAGE;
        }

        Sha1 id = Sha1.randomSha1();
        TrackedWorld trackedWorld = new TrackedWorld(id, name, worldAccessor);

        configuration.trackWorld(trackedWorld);

        repository.saveConfiguration();

        System.out.println("Now tracking world with name "+ trackedWorld.getName() +" and id " + id.asHex());

        return ExitCode.OK;
    }

    @Command(name = "local")
    public int local(
        @Parameters(index = "0", paramLabel = "world directory")
        Path path
    ) throws IOException {
        path = path.toAbsolutePath().normalize();
        WorldAccessor worldAccessor = new DirectWorldProvider(path);
        String name = path.getFileName().toString();

        return this.track(worldAccessor, name);
    }

    @Command(name = "ftp")
    public int ftp(
        @Parameters(index = "0")
        String ftpProfileName,
        @Parameters(index = "1", description = "The path of the world on the remote server.")
        String remotePath
    ) throws IOException {
        MchConfiguration configuration = repository.getConfiguration();
        FtpProfile ftpProfile = configuration.getFtpProfile(ftpProfileName);
        if (ftpProfile == null) {
            System.err.println("No FTP profile with the name \"" + ftpProfileName + "\" exists.");
            System.err.println("Add it using \"mch ftp add " + ftpProfileName + "\"");
            return ExitCode.USAGE;
        }

        FtpWorldAccessor worldAccessor = new FtpWorldAccessor(ftpProfileName, remotePath);
        String name = remotePath;
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        name = name.substring(name.lastIndexOf('/') + 1);

        return this.track(worldAccessor, name);
    }
}
