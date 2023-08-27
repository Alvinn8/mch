package ca.bkaw.mch.cli.ftp;

import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.world.ftp.FtpProfile;
import com.google.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

import java.io.IOException;
import java.util.Scanner;
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

        FtpProfile oldProfile = configuration.getFtpProfile(profileName);
        if (oldProfile == null) {
            System.err.println("No FTP profile with the name " + profileName);
            return ExitCode.USAGE;
        }

        Scanner scanner = new Scanner(System.in);

        System.out.print("Host (" + oldProfile.host() + "): ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) {
            host = oldProfile.host();
        }

        System.out.print("Port (" + oldProfile.port() + "): ");
        String portLine = scanner.nextLine().trim();
        int port = portLine.isEmpty() ? oldProfile.port() : Integer.parseInt(portLine);

        System.out.print("Use FTP over TLS (FTPS)? (" + (oldProfile.secure() ? "yes" : "no") + "): ");
        String secureLine = scanner.nextLine().trim();
        boolean secure = oldProfile.secure();
        if (secureLine.equalsIgnoreCase("yes")
            || secureLine.equalsIgnoreCase("y")) {
            secure = true;
        } else if (secureLine.equalsIgnoreCase("no")
            || secureLine.equalsIgnoreCase("n")) {
            secure = false;
        }

        System.out.print("Username (" + oldProfile.username() + "): ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = oldProfile.username();
        }

        System.out.println("Note: The ftp password will be saved as plain text");
        System.out.println("      in the mch configuration file. Make sure there");
        System.out.println("      is no untrusted access to the mch repository.");
        System.out.print("Password (" + oldProfile.password() + "): ");
        String password = scanner.nextLine().trim();
        if (password.isEmpty()) {
            password = oldProfile.password();
        }

        FtpProfile profile = new FtpProfile(
            host, port, secure,
            username, password
        );

        System.out.println("Attempting to connect now...");

        try {
            profile.tryConnect();
            System.out.println("Connected successfully.");
        } catch (IOException e) {
            System.out.println("Failed to connect: " + e.getMessage());
            System.out.println("Do you still want to save the changes to the FTP profile or do you want to cancel?");
            System.out.println("Cancel (yes, no, default: yes)");
            String cancelLine = scanner.nextLine().trim();
            if (!cancelLine.equalsIgnoreCase("no") && !cancelLine.equalsIgnoreCase("n")) {
                return ExitCode.OK;
            }
        }

        configuration.setFtpProfile(profileName, profile);
        repository.saveConfiguration();

        System.out.println("Updated profile with name " + profileName);

        return ExitCode.OK;
    }
}
