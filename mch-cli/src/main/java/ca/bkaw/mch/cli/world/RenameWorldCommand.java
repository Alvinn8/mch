package ca.bkaw.mch.cli.world;

import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "rename")
public class RenameWorldCommand implements Callable<Integer> {
    @Inject
    MchRepository repository;

    @Parameters(index = "0")
    String from;

    @Parameters(index = "1")
    String to;

    @Override
    public Integer call() throws Exception {
        MchConfiguration configuration = this.repository.getConfiguration();

        TrackedWorld trackedWorld = null;
        for (TrackedWorld t : configuration.getTrackedWorlds()) {
            if (t.getName().equals(this.from)) {
                trackedWorld = t;
            }
        }

        if (trackedWorld == null) {
            System.err.println("No world is tracked with the name " + this.from);
            return ExitCode.USAGE;
        }

        trackedWorld.setName(this.to);

        this.repository.saveConfiguration();

        return ExitCode.OK;
    }
}
