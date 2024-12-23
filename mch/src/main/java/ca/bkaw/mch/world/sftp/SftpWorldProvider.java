package ca.bkaw.mch.world.sftp;

import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.util.StringPath;
import ca.bkaw.mch.util.Util;
import ca.bkaw.mch.world.FileInfo;
import ca.bkaw.mch.world.WorldProvider;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.SFTPClient;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class SftpWorldProvider implements WorldProvider {
    private final SSHClient sshClient;
    private final SFTPClient sftp;
    private final StringPath worldPath;

    public SftpWorldProvider(SftpProfile sftpProfile, MchRepository repository, String worldPath) throws IOException {
        this.worldPath = StringPath.of(worldPath);
        this.sshClient = sftpProfile.connect(repository);
        this.sftp = this.sshClient.newSFTPClient();
    }

    private String getPath(StringPath path) {
        return this.worldPath.resolve(Util.noLeadingSlash(path.toString())).toString();
    }

    @Override
    public List<FileInfo> list(StringPath path) throws IOException {
        return this.sftp.ls(this.getPath(path)).stream()
            .map(file -> new FileInfo(
                file.getName(),
                path.resolve(file.getName()),
                new FileInfo.Metadata(
                    file.isRegularFile(),
                    file.isDirectory(),
                    file.getAttributes().getSize(),
                    file.getAttributes().getMtime()
                )
            ))
            .toList();
    }

    @Nullable
    @Override
    public FileInfo.Metadata stat(StringPath path) throws IOException {
        FileAttributes attrs = this.sftp.statExistence(this.getPath(path));
        if (attrs == null) {
            return null;
        }
        return new FileInfo.Metadata(
            attrs.getType() == FileMode.Type.REGULAR,
            attrs.getType() == FileMode.Type.DIRECTORY,
            attrs.getSize(),
            attrs.getMtime()
        );
    }

    @Override
    public RandomAccessReader openFile(StringPath path, long estimatedSize) throws IOException {
        return RandomAccessReader.of(this.readFile(path, estimatedSize));
    }

    @Override
    public byte[] readFile(StringPath path, long estimatedSize) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        this.sftp.get(this.getPath(path), new OutputStreamFileDest(stream));
        return stream.toByteArray();
    }

    @Override
    public void close() throws IOException {
        this.sshClient.close();
        this.sftp.close();
    }
}
