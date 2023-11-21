package ca.bkaw.mch.cli.sftp;

import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "remove")
public class RemoveSftpProfileCommand implements Callable<Integer> {
    @Inject
    MchRepository repository;

    @Parameters(index = "0")
    String profileName;

    @Override
    public Integer call() throws IOException {
        MchConfiguration configuration = repository.getConfiguration();
        if (configuration.getSftpProfiles().getProfile(profileName) == null) {
            System.err.println("No SFTP profile with the name " + profileName);
            return ExitCode.OK;
        }
        configuration.getSftpProfiles().remove(profileName);
        repository.saveConfiguration();
        System.out.println(profileName + " was removed");
        return ExitCode.OK;
    }
}
