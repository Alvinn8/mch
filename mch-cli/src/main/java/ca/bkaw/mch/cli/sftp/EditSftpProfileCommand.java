package ca.bkaw.mch.cli.sftp;


import ca.bkaw.mch.cli.ProfilePrompt;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.world.sftp.SftpProfile;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "edit")
public class EditSftpProfileCommand implements Callable<Integer> {
    @Inject
    MchRepository repository;

    @Parameters(index = "0")
    String profileName;

    @Override
    public Integer call() throws IOException {
        MchConfiguration configuration = repository.getConfiguration();

        SftpProfile oldProfile = configuration.getSftpProfiles().getProfile(profileName);
        if (oldProfile == null) {
            System.err.println("No SFTP profile with the name " + profileName);
            return ExitCode.USAGE;
        }

        ProfilePrompt prompt = new ProfilePrompt();
        prompt.setDefaultHost(oldProfile.host());
        prompt.setDefaultPort(oldProfile.port());
        prompt.setDefaultUsername(oldProfile.username());
        prompt.setDefaultPassword(oldProfile.password());
        prompt.run();

        SftpProfile profile = new SftpProfile(
            prompt.host(), prompt.port(),
            prompt.username(), prompt.password()
        );

        if (!AddSftpProfileCommand.testConnection(prompt, repository, profile)) {
            return ExitCode.OK;
        }

        configuration.getSftpProfiles().setProfile(profileName, profile);
        repository.saveConfiguration();

        System.out.println("Updated profile with name " + profileName);

        return ExitCode.OK;
    }

}
