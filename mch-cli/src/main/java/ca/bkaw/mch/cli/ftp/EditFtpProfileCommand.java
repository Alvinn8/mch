package ca.bkaw.mch.cli.ftp;

import ca.bkaw.mch.cli.ProfilePrompt;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.world.ftp.FtpProfile;
import com.google.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "edit")
public class EditFtpProfileCommand implements Callable<Integer> {
    @Inject
    MchRepository repository;

    @CommandLine.Parameters(index = "0")
    String profileName;

    @Override
    public Integer call() throws IOException {

        MchConfiguration configuration = repository.getConfiguration();

        FtpProfile oldProfile = configuration.getFtpProfiles().getProfile(profileName);
        if (oldProfile == null) {
            System.err.println("No FTP profile with the name " + profileName);
            return ExitCode.OK;
        }

        ProfilePrompt prompt = new ProfilePrompt();
        prompt.setDefaultHost(oldProfile.host());
        prompt.setDefaultPort(oldProfile.port());
        prompt.setDefaultPassword(oldProfile.password());
        prompt.setDefaultUsername(oldProfile.username());
        prompt.setDefaultSecure(oldProfile.secure());
        prompt.setAskForSecureFTP();
        prompt.run();

        FtpProfile profile = new FtpProfile(
            prompt.host(), prompt.port(), prompt.secure(),
            prompt.username(), prompt.password()
        );

        if (!AddFtpProfileCommand.testConnection(prompt, profile)) {
            return ExitCode.OK;
        }

        configuration.getFtpProfiles().setProfile(profileName, profile);
        repository.saveConfiguration();

        System.out.println("Updated profile with name " + profileName);

        return ExitCode.OK;
    }
}
