package ca.bkaw.mch.cli;

import ca.bkaw.mch.repository.MchRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "init")
public class InitCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        Path mchPath = Path.of("mch");
        if (Files.exists(mchPath)) {
            System.err.println("There is already an mch repository in this folder.");
            return ExitCode.USAGE;
        }
        MchRepository repository = new MchRepository(mchPath);
        try {
            repository.createDirectories();
            repository.readConfiguration();
            repository.saveConfiguration();
            repository.setHeadCommit(null);
        } catch (IOException e) {
            System.err.println("Failed to initialize mch repository.");
            e.printStackTrace();
            return ExitCode.SOFTWARE;
        }

        System.out.println("Initialized a new mch repository.");
        return ExitCode.OK;
    }
}
