package ca.bkaw.mch.world.sftp;

import ca.bkaw.mch.repository.MchRepository;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.ConsoleKnownHostsVerifier;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;

import java.io.Console;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Path;

public record SftpProfile(
    String host, int port,
    String username, String password
) {
    public SftpProfile(DataInput dataInput) throws IOException {
        this(
            dataInput.readUTF(), dataInput.readInt(),
            dataInput.readUTF(), dataInput.readUTF()
        );
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.host);
        dataOutput.writeInt(this.port);
        dataOutput.writeUTF(this.username);
        dataOutput.writeUTF(this.password);
    }

    public SSHClient connect(MchRepository mchRepository) throws IOException {
        Path knownHostsPath = mchRepository.getRoot().resolve("known_hosts");
        SSHClient sshClient = new SSHClient();

        Console console = System.console();
        HostKeyVerifier hostKeyVerifier = console != null
            ? new ConsoleKnownHostsVerifier(knownHostsPath.toFile(), console)
            : new OpenSSHKnownHosts(knownHostsPath.toFile());

        sshClient.addHostKeyVerifier(hostKeyVerifier);
        sshClient.connect(this.host, this.port);
        sshClient.authPassword(this.username, this.password);
        return sshClient;
    }

    public void tryConnect(MchRepository mchRepository) throws IOException {
        try (SSHClient sshClient = this.connect(mchRepository)) {
            SFTPClient sftp = sshClient.newSFTPClient();
            sftp.ls("");
        }
    }
}
