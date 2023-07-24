package ca.bkaw.mch.repository;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.world.DirectWorldProvider;
import ca.bkaw.mch.world.WorldProvider;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class TrackedWorld {
    private final Sha1 id;
    private final WorldProvider worldProvider;

    public TrackedWorld(Sha1 id, WorldProvider worldProvider) {
        this.id = id;
        this.worldProvider = worldProvider;
    }

    public TrackedWorld(DataInput dataInput) throws IOException {
        this.id = Sha1.read(dataInput);
        byte worldProviderType = dataInput.readByte();
        if (worldProviderType != 1) {
            throw new RuntimeException("Only world provider type 1 is supported");
        }
        this.worldProvider = new DirectWorldProvider(dataInput);
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.write(this.id.getBytes());
        dataOutput.writeByte(1);
        ((DirectWorldProvider) this.worldProvider).write(dataOutput);
    }

    public Sha1 getId() {
        return this.id;
    }

    public WorldProvider getWorldProvider() {
        return this.worldProvider;
    }
}
