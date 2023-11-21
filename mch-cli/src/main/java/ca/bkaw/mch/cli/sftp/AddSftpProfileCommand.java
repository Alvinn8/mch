package ca.bkaw.mch.cli.sftp;

import ca.bkaw.mch.cli.ProfilePrompt;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.world.sftp.SftpProfile;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;

@Command(name = "add")
public class AddSftpProfileCommand implements Runnable {
    @Inject
    MchRepository repository;

    @Parameters(index = "0")
    String profileName;

    @Override
    public void run() {
        ProfilePrompt prompt = new ProfilePrompt();
        prompt.setDefaultPort(22);
        prompt.run();

        MchConfiguration configuration = repository.getConfiguration();
        if (configuration.getSftpProfiles().getProfile(profileName) != null) {
            if (!prompt.askShouldOverride()) {
                return;
            }
        }

        SftpProfile profile = new SftpProfile(
            prompt.host(), prompt.port(),
            prompt.username(), prompt.password()
        );

        if (!testConnection(prompt, repository, profile)) {
            return;
        }

        configuration.getSftpProfiles().setProfile(profileName, profile);

        try {
            repository.saveConfiguration();
        } catch (IOException e) {
            System.err.println("Failed to save configuration.");
            e.printStackTrace();
            return;
        }

        System.out.println("Added profile with name " + profileName);
    }

    public static boolean testConnection(ProfilePrompt prompt, MchRepository repository, SftpProfile profile) {
        System.out.println("Attempting to connect now...");
        System.out.println("Note: The known-hosts file used is located inside the mch folder.");

        try {
            profile.tryConnect(repository);
            System.out.println("Connected successfully.");
        } catch (IOException e) {
            if (!prompt.confirmWhenFailedToConnect(e)) {
                return false;
            }
        }
        return true;
    }
}
