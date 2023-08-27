package ca.bkaw.mch.world.ftp;

import ca.bkaw.mch.world.WorldAccessor;
import ca.bkaw.mch.world.WorldProvider;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link WorldAccessor} that connects to an FTP server to access the world.
 */
public class FtpWorldAccessor implements WorldAccessor {
    public static final byte ID = 2;

    private final FtpProfile ftpProfile;
    private final String worldPath;

    public FtpWorldAccessor(FtpProfile ftpProfile, String worldPath) {
        this.ftpProfile = ftpProfile;
        this.worldPath = worldPath;
    }

    public FtpWorldAccessor(DataInput dataInput) throws IOException {
        this.ftpProfile = new FtpProfile(dataInput);
        this.worldPath = dataInput.readUTF();
    }

    @Override
    public WorldProvider access() throws IOException {
        return new FtpWorldProvider(this.ftpProfile, this.worldPath);
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        this.ftpProfile.write(dataOutput);
        dataOutput.writeUTF(this.worldPath);
    }

    @Override
    public byte getId() {
        return ID;
    }

    /**
     * A simple attempt at normalizing similar world paths. The result of this method
     * should only be used in {@link #equals(Object)} and {@link #hashCode()}.
     * <p>
     * This is so that if the user tries to add one world with the path {@code world}
     * and one that is {@code ./world} or {@code world/}, they are detected as
     * referencing the same directory.
     *
     * @return The normalized world path.
     */
    private String normalizedWorldPath() {
        String worldPath = this.worldPath;
        if (worldPath.endsWith("/")) {
            worldPath = worldPath.substring(0, worldPath.length() - 1);
        }
        if (worldPath.startsWith("./")) {
            worldPath = worldPath.substring("./".length());
        }
        return worldPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FtpWorldAccessor that = (FtpWorldAccessor) o;

        if (!this.ftpProfile.equals(that.ftpProfile)) return false;
        return this.normalizedWorldPath().equals(that.normalizedWorldPath());
    }

    @Override
    public int hashCode() {
        int result = this.ftpProfile.hashCode();
        result = 31 * result + this.normalizedWorldPath().hashCode();
        return result;
    }
}
