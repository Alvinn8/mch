package ca.bkaw.mch.cli;

import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.repository.MchRepository;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

@Command(name = "log")
public class LogCommand implements Callable<Integer> {
    public static final int MAX_RESULTS = 10;

    @Inject
    MchRepository repository;

    @Override
    public Integer call() throws IOException {
        Reference20<Commit> commitReference = repository.getHeadCommit();

        if (commitReference == null) {
            System.out.println("No commits");
        }

        int count = 0;
        while (commitReference != null && count < MAX_RESULTS) {
            Commit commit = commitReference.resolve(repository);

            System.out.println("commit " + commitReference.getSha1().asHex());
            System.out.println("  Date: " + DateFormat.getInstance().format(new Date(commit.getTime())));
            String message = commit.getMessage();
            if (!message.isEmpty()) {
                System.out.println("  Message: " + message);
            }
            System.out.println();

            commitReference = commit.getPreviousCommit();

            count++;
        }

        return ExitCode.OK;
    }
}
