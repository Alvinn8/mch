package ca.bkaw.mch.cli.world;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.world.DirectWorldProvider;
import ca.bkaw.mch.world.WorldAccessor;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "add")
public class AddWorldCommand implements Callable<Integer> {
    @Inject
    MchRepository repository;

    @Parameters(index = "0", paramLabel = "world directory")
    Path path;

    @Override
    public Integer call() throws IOException {
        MchConfiguration configuration = repository.getConfiguration();

        path = path.toAbsolutePath().normalize();
        WorldAccessor worldAccessor = new DirectWorldProvider(path);

        if (configuration.alreadyTracking(worldAccessor)) {
            System.err.println("That world is already being tracked my this repository.");
            return ExitCode.USAGE;
        }

        Sha1 id = Sha1.randomSha1();
        String name = path.getFileName().toString();
        TrackedWorld trackedWorld = new TrackedWorld(id, name, worldAccessor);

        configuration.trackWorld(trackedWorld);

        repository.saveConfiguration();

        System.out.println("Now tracking world with name "+ trackedWorld.getName() +" and id " + id.asHex());

        return ExitCode.OK;
    }
}
