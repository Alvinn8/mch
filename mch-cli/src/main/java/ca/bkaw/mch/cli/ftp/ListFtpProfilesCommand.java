package ca.bkaw.mch.cli.ftp;

import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.world.ftp.FtpProfile;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;

@Command(name = "list")
public class ListFtpProfilesCommand implements Runnable {
    @Inject
    MchRepository repository;

    @Option(names = {"-v", "--verbose"}, defaultValue = "false")
    boolean verbose;

    @Override
    public void run() {
        MchConfiguration configuration = repository.getConfiguration();

        Map<String, FtpProfile> profiles = configuration.getFtpProfiles();
        System.out.println(
            profiles.size() + " FTP " + (profiles.size() == 1 ? "profile" : "profiles") + ":"
        );
        for (Map.Entry<String, FtpProfile> entry : profiles.entrySet()) {
            System.out.print(entry.getKey());
            if (verbose) {
                FtpProfile profile = entry.getValue();
                System.out.print(" - " + profile.host() + " as " + profile.username());
            }
            System.out.println();
        }
    }
}
