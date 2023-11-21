package ca.bkaw.mch.world.sftp;

import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.world.WorldAccessor;
import ca.bkaw.mch.world.WorldProvider;
import ca.bkaw.mch.world.ftp.FtpWorldAccessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SftpWorldAccessor implements WorldAccessor {
    public static final byte ID = 4;

    private final String sftpProfileName;
    private final String worldPath;

    public SftpWorldAccessor(String sftpProfileName, String worldPath) {
        this.sftpProfileName = sftpProfileName;
        this.worldPath = worldPath;
    }

    public SftpWorldAccessor(DataInput dataInput) throws IOException {
        this.sftpProfileName = dataInput.readUTF();
        this.worldPath = dataInput.readUTF();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.sftpProfileName);
        dataOutput.writeUTF(this.worldPath);
    }

    @Override
    public WorldProvider access(MchRepository mchRepository) throws IOException {
        SftpProfile sftpProfile = mchRepository.getConfiguration().getSftpProfiles().getProfile(this.sftpProfileName);
        if (sftpProfile == null) {
            throw new RuntimeException("No SFTP profile with the name \"" + this.sftpProfileName + "\" exists.");
        }
        System.out.println("Connecting...");
        return new SftpWorldProvider(sftpProfile, mchRepository, this.worldPath);
    }

    @Override
    public byte getId() {
        return ID;
    }

    private String normalizedWorldPath() {
        return FtpWorldAccessor.normalizedWorldPath(this.worldPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SftpWorldAccessor that = (SftpWorldAccessor) o;

        if (!this.sftpProfileName.equals(that.sftpProfileName)) return false;
        return this.normalizedWorldPath().equals(that.normalizedWorldPath());
    }

    @Override
    public int hashCode() {
        int result = this.sftpProfileName.hashCode();
        result = 31 * result + this.normalizedWorldPath().hashCode();
        return result;
    }
}
