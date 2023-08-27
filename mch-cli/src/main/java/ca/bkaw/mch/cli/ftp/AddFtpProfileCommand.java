package ca.bkaw.mch.cli.ftp;

import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.world.ftp.FtpProfile;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.Scanner;

@Command(name = "add")
public class AddFtpProfileCommand implements Runnable {
    @Inject
    MchRepository repository;

    @Parameters(index = "0")
    String profileName;

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Host: ");
        String host = scanner.nextLine().trim();

        System.out.print("Port (default 21): ");
        String portLine = scanner.nextLine().trim();
        int port = portLine.isEmpty() ? 21 : Integer.parseInt(portLine);

        System.out.print("Use FTP over TLS (FTPS)? (yes, no, default: no): ");
        String secureLine = scanner.nextLine().trim();
        boolean secure = secureLine.equalsIgnoreCase("yes")
            || secureLine.equalsIgnoreCase("y");

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.println("Note: The ftp password will be saved as plain text");
        System.out.println("      in the mch configuration file. Make sure there");
        System.out.println("      is no untrusted access to the mch repository.");
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        MchConfiguration configuration = repository.getConfiguration();
        if (configuration.getFtpProfile(profileName) != null) {
            System.out.println("A profile with that name already exists, do you want to override it? (yes, no, default: yes)");
            String line = scanner.nextLine().trim();
            if (!"y".equalsIgnoreCase(line) && !"yes".equalsIgnoreCase(line)) {
                System.out.println("Cancelled.");
                return;
            }
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
            System.out.println("Do you still want to add the FTP profile or do you want to cancel?");
            System.out.println("Cancel (yes, no, default: yes)");
            String cancelLine = scanner.nextLine().trim();
            if (!cancelLine.equalsIgnoreCase("no") && !cancelLine.equalsIgnoreCase("n")) {
                return;
            }
        }

        configuration.setFtpProfile(profileName, profile);

        try {
            repository.saveConfiguration();
        } catch (IOException e) {
            System.err.println("Failed to save configuration.");
            e.printStackTrace();
            return;
        }

        System.out.println("Added profile with name " + profileName);
    }
}
