package ca.bkaw.mch.world.zip;

import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.world.WorldAccessor;
import ca.bkaw.mch.world.WorldProvider;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A {@link WorldAccessor} that opens a zip file for accessing a world.
 */
public class ZipWorldAccessor implements WorldAccessor {
    public static final byte ID = 3;

    private final Path zipPath;
    private final String pathInsideZip;

    public ZipWorldAccessor(Path zipPath, String pathInsideZip) {
        this.zipPath = zipPath;
        this.pathInsideZip = pathInsideZip;
    }

    public ZipWorldAccessor(DataInput dataInput) throws IOException {
        String zipPath = dataInput.readUTF();
        this.zipPath = Path.of(zipPath);
        this.pathInsideZip = dataInput.readUTF();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.zipPath.toAbsolutePath().toString());
        dataOutput.writeUTF(this.pathInsideZip);
    }

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public WorldProvider access(MchRepository mchRepository) throws IOException {
        return new ZipWorldProvider(this.zipPath, this.pathInsideZip);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZipWorldAccessor that = (ZipWorldAccessor) o;

        if (!zipPath.equals(that.zipPath)) return false;
        return pathInsideZip.equals(that.pathInsideZip);
    }

    @Override
    public int hashCode() {
        int result = zipPath.hashCode();
        result = 31 * result + pathInsideZip.hashCode();
        return result;
    }
}
