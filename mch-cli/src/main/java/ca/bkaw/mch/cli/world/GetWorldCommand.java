package ca.bkaw.mch.cli.world;

import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "get")
public class GetWorldCommand implements Callable<Integer> {
    @Inject
    MchRepository repository;

    @Parameters(index = "0", description = "world name or id")
    String identifier;

    @Override
    public Integer call() {
        MchConfiguration configuration = this.repository.getConfiguration();

        for (TrackedWorld trackedWorld : configuration.getTrackedWorlds()) {
            if (this.identifier.equals(trackedWorld.getName())
                || this.identifier.equals(trackedWorld.getId().asHex())) {
                System.out.println("name: " + trackedWorld.getName());
                System.out.println("id: " + trackedWorld.getId().asHex());
                return ExitCode.OK;
            }
        }
        return ExitCode.USAGE;
    }
}
