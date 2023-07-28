package ca.bkaw.mch.cli;

import ca.bkaw.mch.command.CommitOperation;
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

    @Override
    public void run() {
        try {
            CommitOperation.run(this.repository, this.commitMessage);
        } catch (IOException e) {
            System.err.println("Failed to commit.");
            e.printStackTrace();
        }
    }
}
