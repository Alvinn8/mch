package ca.bkaw.mch.cli;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Scanner;

/**
 * Utility for prompting the user when adding and editing profiles.
 */
public class ProfilePrompt {
    private final Scanner scanner = new Scanner(System.in);
    private String host;
    private int port;
    private boolean askForSecure = false;
    private boolean secure;
    private String username;
    private String password;

    public void setDefaultHost(String defaultHost) { this.host = defaultHost; }
    public void setDefaultPort(int defaultPort) { this.port = defaultPort; }
    public void setAskForSecureFTP() { this.askForSecure = true; }
    public void setDefaultSecure(boolean defaultSecure) { this.secure = defaultSecure; }
    public void setDefaultUsername(String defaultUsername) { this.username = defaultUsername; }
    public void setDefaultPassword(String defaultPassword) { this.password = defaultPassword; }

    public String host() { return host; }
    public int port() { return port; }
    public boolean secure() { return secure; }
    public String username() { return username; }
    public String password() { return password; }

    private String format(String name, @Nullable String defaultValue) {
        if (defaultValue == null) {
            return name + ": ";
        }
        return name + " (" + defaultValue + "): ";
    }

    public void run() {
        System.out.println("Press enter to use values in parentheses");

        System.out.print(format("Host", this.host));
        String host = scanner.nextLine().trim();
        if (!host.isEmpty()) {
            this.host = host;
        }

        System.out.print(format("Port", String.valueOf(this.port)));
        String portLine = scanner.nextLine().trim();
        if (!portLine.isEmpty()) {
            this.port = Integer.parseInt(portLine);
        }

        if (this.askForSecure) {
            System.out.print("Use FTP over TLS (FTPS)? (yes, no, default: no): ");
            String secureLine = scanner.nextLine().trim();
            if (secureLine.equalsIgnoreCase("yes")
                || secureLine.equalsIgnoreCase("y")) {
                this.secure = true;
            } else if (secureLine.equalsIgnoreCase("no")
                || secureLine.equalsIgnoreCase("n")) {
                this.secure = false;
            }
        }

        System.out.print(format("Username", this.username));
        String username = scanner.nextLine().trim();
        if (!username.isEmpty()) {
            this.username = username;
        }

        System.out.println("Note: The password will be saved as plain text in");
        System.out.println("      the mch configuration file. Make sure there");
        System.out.println("      is no untrusted access to the mch repository.");
        System.out.print(format("Password", this.password));
        String password = scanner.nextLine().trim();
        if (!password.isEmpty()) {
            this.password = password;
        }
    }

    public boolean askShouldOverride() {
        System.out.println("A profile with that name already exists, do you want to override it? (yes, no, default: yes)");
        String line = scanner.nextLine().trim();
        if (!"y".equalsIgnoreCase(line) && !"yes".equalsIgnoreCase(line)) {
            System.out.println("Cancelled.");
            return false;
        }
        return true;
    }

    public boolean confirmWhenFailedToConnect(IOException e) {
        System.out.println("Failed to connect: " + e.getMessage());
        System.out.println("Do you still want to add the profile or do you want to cancel?");
        System.out.println("Cancel (yes, no, default: yes)");
        String cancelLine = scanner.nextLine().trim();
        if (!cancelLine.equalsIgnoreCase("no") && !cancelLine.equalsIgnoreCase("n")) {
            System.out.println("Cancelled.");
            return false;
        }
        return true;
    }
}
