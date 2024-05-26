package ca.bkaw.mch.cli;

import ca.bkaw.mch.operation.CommitOperation;
import ca.bkaw.mch.repository.MchRepository;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;

@Command(name = "commit")
public class CommitCommand implements Runnable {
    @Inject
    MchRepository repository;

    @Option(names = { "--message", "-m" })
    String commitMessage;

    @Option(names = "--no-cache", negatable = true, defaultValue = "true", description = "Skip caching by looking at the current commit.")
    boolean cache;

    @Option(names = "--verbose", defaultValue = "false", description = "Print more information while processing the commit.")
    boolean verbose;

    @Override
    public void run() {
        try {
            CommitOperation.run(this.repository, this.commitMessage, this.cache, this.verbose);
        } catch (IOException e) {
            System.err.println("Failed to commit.");
            e.printStackTrace();
        }
    }
}
