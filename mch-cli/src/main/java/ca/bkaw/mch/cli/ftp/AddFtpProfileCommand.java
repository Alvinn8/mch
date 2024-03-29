package ca.bkaw.mch.cli.ftp;

import ca.bkaw.mch.cli.ProfilePrompt;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.world.ftp.FtpProfile;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;

@Command(name = "add")
public class AddFtpProfileCommand implements Runnable {
    @Inject
    MchRepository repository;

    @Parameters(index = "0")
    String profileName;

    @Override
    public void run() {
        ProfilePrompt prompt = new ProfilePrompt();
        prompt.setDefaultPort(21);
        prompt.setAskForSecureFTP();
        prompt.run();

        MchConfiguration configuration = repository.getConfiguration();
        if (configuration.getFtpProfiles().getProfile(profileName) != null) {
            if (!prompt.askShouldOverride()) {
                return;
            }
        }

        FtpProfile profile = new FtpProfile(
            prompt.host(), prompt.port(), prompt.secure(),
            prompt.username(), prompt.password()
        );

        System.out.println("Attempting to connect now...");

        if (!testConnection(prompt, profile)) {
            return;
        }

        configuration.getFtpProfiles().setProfile(profileName, profile);

        try {
            repository.saveConfiguration();
        } catch (IOException e) {
            System.err.println("Failed to save configuration.");
            e.printStackTrace();
            return;
        }

        System.out.println("Added profile with name " + profileName);
    }

    public static boolean testConnection(ProfilePrompt prompt, FtpProfile profile) {
        try {
            profile.tryConnect();
            System.out.println("Connected successfully.");
        } catch (IOException e) {
            if (!prompt.confirmWhenFailedToConnect(e)) {
                return false;
            }
        }
        return true;
    }
}
