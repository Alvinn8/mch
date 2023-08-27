package ca.bkaw.mch.world.ftp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A profile for connecting to a remote FTP server.
 *
 * @param host The hostname of the ftp server.
 * @param port The port to connect to.
 * @param secure Whether to use FTP over TLS (FTPS).
 * @param username The username to connect as.
 * @param password The password.
 */
public record FtpProfile(
    String host, int port, boolean secure,
    String username, String password
) {
    public FtpProfile(DataInput dataInput) throws IOException {
        this(
            dataInput.readUTF(), dataInput.readInt(), dataInput.readBoolean(),
            dataInput.readUTF(), dataInput.readUTF()
        );
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.host);
        dataOutput.writeInt(this.port);
        dataOutput.writeBoolean(this.secure);
        dataOutput.writeUTF(this.username);
        dataOutput.writeUTF(this.password);
    }
}
