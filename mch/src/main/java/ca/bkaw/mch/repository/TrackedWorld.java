package ca.bkaw.mch.repository;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.world.DirectWorldProvider;
import ca.bkaw.mch.world.WorldAccessor;
import ca.bkaw.mch.world.ftp.FtpWorldAccessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class TrackedWorld {
    private final Sha1 id;
    private String name;
    private final WorldAccessor worldAccessor;

    public TrackedWorld(Sha1 id, String name, WorldAccessor worldAccessor) {
        this.id = id;
        this.name = name;
        this.worldAccessor = worldAccessor;
    }

    public TrackedWorld(DataInput dataInput) throws IOException {
        this.id = Sha1.read(dataInput);
        this.name = dataInput.readUTF();
        byte worldAccessorType = dataInput.readByte();
        this.worldAccessor = switch (worldAccessorType) {
            case DirectWorldProvider.ID -> new DirectWorldProvider(dataInput);
            case FtpWorldAccessor.ID -> new FtpWorldAccessor(dataInput);
            default -> throw new RuntimeException("Unknown world accessor type " + worldAccessorType);
        };
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.write(this.id.getBytes());
        dataOutput.writeUTF(this.name);
        dataOutput.writeByte(this.worldAccessor.getId());
        this.worldAccessor.write(dataOutput);
    }

    public Sha1 getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WorldAccessor getWorldAccessor() {
        return this.worldAccessor;
    }
}
