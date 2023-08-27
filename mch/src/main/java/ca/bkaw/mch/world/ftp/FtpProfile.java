package ca.bkaw.mch.world.ftp;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;

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

    public FTPClient connect() throws IOException {
        FTPClient ftp = this.secure ? new FTPSClient() : new FTPClient();

        ftp.connect(this.host, this.port);
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            throw new IOException("Failed to connect to FTP Server");
        }

        if (false) {

            class SpyWriter extends BufferedWriter {

                public SpyWriter(@NotNull Writer out) {
                    super(out);
                }

                @Override
                public void write(@NotNull String str) throws IOException {
                    super.write(str);
                    if (str.endsWith("\n")) {
                        str = str.substring(0, str.length() - 1);
                    }
                    System.out.println("FTP > " + str);
                }
            }

            class SpyReader extends BufferedReader {

                public SpyReader(@NotNull Reader in) {
                    super(in);
                }

                @Override
                public String readLine() throws IOException {
                    String line = super.readLine();
                    System.out.println("FTP < " + line);
                    return line;
                }
            }

            try {
                Field f1 = FTP.class.getDeclaredField("_controlOutput_");
                f1.setAccessible(true);
                BufferedWriter actualWriter = (BufferedWriter) f1.get(ftp);
                f1.set(ftp, new SpyWriter(actualWriter));

                Field f2 = FTP.class.getDeclaredField("_controlInput_");
                f2.setAccessible(true);
                BufferedReader actualReader = (BufferedReader) f2.get(ftp);
                f2.set(ftp, new SpyReader(actualReader));
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }

        }

        boolean loginSuccess = ftp.login(this.username, this.password);

        if (!loginSuccess) {
            throw new IOException("Failed to log in to FTP server. Incorrect username or password?");
        }

        // Use local passive mode so that the server opens a port that the client
        // connects to instead of requiring the client to open a port, which due
        // to firewalls usually doesn't work.
        ftp.enterLocalPassiveMode();

        return ftp;
    }

    /**
     * Test the connectivity of this profile by connecting, listing, and disconnecting.
     *
     * @throws IOException If the connection could not be established.
     */
    public void tryConnect() throws IOException {
        FTPClient ftpClient = this.connect();
        try {
            ftpClient.listFiles();
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                throw new IOException("Unable to list files. " + ftpClient.getReplyString());
            }
        } finally {
            ftpClient.disconnect();
        }
    }
}
