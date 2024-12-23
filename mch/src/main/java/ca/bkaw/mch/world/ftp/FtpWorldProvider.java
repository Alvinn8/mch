package ca.bkaw.mch.world.ftp;

import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.util.StringPath;
import ca.bkaw.mch.util.Util;
import ca.bkaw.mch.world.FileInfo;
import ca.bkaw.mch.world.WorldProvider;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * An active connection to an external FTP server that provides information about
 * a world by reading from an external FTP server.
 * <p>
 * Close the object to disconnect from the FTP server.
 */
public class FtpWorldProvider implements WorldProvider {
    private final FTPClient ftp;
    private final StringPath worldPath;

    /**
     * Create a new provider and connect to the FTP server.
     *
     * @param profile Information on how to connect to the FTP server.
     * @param worldPath The path where the world can be found on the remote server.
     * @throws IOException If an I/O error occurs while connecting.
     */
    public FtpWorldProvider(FtpProfile profile, String worldPath) throws IOException {
        this.ftp = profile.connect();

        // Ensure files are sent in binary to avoid line ending being changed for
        // binary files.
        this.ftp.setFileType(FTP.BINARY_FILE_TYPE);

        this.ftp.changeWorkingDirectory(worldPath);
        this.worldPath = StringPath.of(this.ftp.printWorkingDirectory());
    }

    private String getPath(StringPath path) {
        String ret = this.worldPath.resolve(Util.noLeadingSlash(path.toString())).toString();
        System.out.println("ret = " + ret);
        return ret;
    }

    @Override
    public List<FileInfo> list(StringPath path) throws IOException {
        return Arrays.stream(this.ftp.listFiles(this.getPath(path)))
            .map(file -> new FileInfo(
                file.getName(),
                path.resolve(file.getName()),
                new FileInfo.Metadata(
                    file.isFile(),
                    file.isDirectory(),
                    file.getSize(),
                    file.getTimestampInstant().toEpochMilli()
                )
            ))
            .toList();
    }

    @Nullable
    @Override
    public FileInfo.Metadata stat(StringPath path) throws IOException {
        // TODO this is a bit hacky... ftp has no good way of getting whether a file/folder exists.
        FTPFile[] ftpFiles = this.ftp.listFiles(this.getPath(path));
        if (ftpFiles == null) {
            return null;
        }
        if (ftpFiles.length == 1) {
            FTPFile ftpFile = ftpFiles[0];
            return new FileInfo.Metadata(
                ftpFile.isFile(),
                ftpFile.isDirectory(),
                ftpFile.getSize(),
                ftpFile.getTimestampInstant().toEpochMilli()
            );
        }
        if (ftpFiles.length > 1) {
            return new FileInfo.Metadata(
                false,
                true,
                -1,
                -1
            );
        }
        // ?? Maybe an empty folder? Or it doesn't exist. I don't know.
        return null;
    }

    @Override
    public RandomAccessReader openFile(StringPath path, long estimatedSize) throws IOException {
        return RandomAccessReader.of(this.readFile(path, estimatedSize));
    }

    @Override
    public byte[] readFile(StringPath path, long estimatedSize) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream((int) estimatedSize);
        this.ftp.retrieveFile(this.getPath(path), stream);
        return stream.toByteArray();
    }

    @Override
    public void close() throws IOException {
        this.ftp.disconnect();
    }
}
