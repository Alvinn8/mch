package ca.bkaw.mch.cli.world;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.world.DirectWorldProvider;
import ca.bkaw.mch.world.WorldProvider;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;

@Command(name = "add")
public class AddWorldCommand implements Runnable {
    @Inject
    MchRepository repository;

    @Parameters(index = "0", paramLabel = "world directory")
    Path path;

    @Override
    public void run() {
        MchConfiguration configuration = repository.getConfiguration();

        path = path.normalize();
        WorldProvider worldProvider = new DirectWorldProvider(path);

        Sha1 id = Sha1.randomSha1();
        String name = path.getFileName().toString();
        TrackedWorld trackedWorld = new TrackedWorld(id, name, worldProvider);

        configuration.trackWorld(trackedWorld);

        try {
            repository.saveConfiguration();
        } catch (IOException e) {
            System.err.println("Failed to save configuration.");
            e.printStackTrace();
            return;
        }

        System.out.println("Now tracking world with id " + id.asHex());
    }
}
