package ca.bkaw.mch.cli.sftp;

import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.world.sftp.SftpProfile;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;

@Command(name = "list")
public class ListSftpProfilesCommand implements Runnable {
    @Inject
    MchRepository repository;

    @Option(names = {"-v", "--verbose"}, defaultValue = "false")
    boolean verbose;

    @Override
    public void run() {
        MchConfiguration configuration = repository.getConfiguration();

        Map<String, SftpProfile> profiles = configuration.getSftpProfiles().getProfiles();
        System.out.println(
            profiles.size() + " SFTP " + (profiles.size() == 1 ? "profile" : "profiles") + ":"
        );
        for (Map.Entry<String, SftpProfile> entry : profiles.entrySet()) {
            System.out.print(entry.getKey());
            if (verbose) {
                SftpProfile profile = entry.getValue();
                System.out.print(" - " + profile.host() + ":" + profile.port() + " as " + profile.username());
            }
            System.out.println();
        }
    }
}