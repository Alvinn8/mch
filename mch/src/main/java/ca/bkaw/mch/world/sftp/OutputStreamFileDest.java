package ca.bkaw.mch.world.sftp;

import net.schmizz.sshj.xfer.InMemoryDestFile;

import java.io.OutputStream;

/**
 * A {@link InMemoryDestFile} that is used to download SFTP files to an
 * {@link OutputStream}.
 */
public class OutputStreamFileDest extends InMemoryDestFile {
    private final OutputStream outputStream;

    public OutputStreamFileDest(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public long getLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    @Override
    public OutputStream getOutputStream(boolean append) {
        if (append) {
            throw new UnsupportedOperationException();
        }
        return this.getOutputStream();
    }
}
