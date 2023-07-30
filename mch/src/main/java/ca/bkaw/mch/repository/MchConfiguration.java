package ca.bkaw.mch.repository;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.Sha1;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MchConfiguration {
    public static final int MAGIC = FileMagic.CONFIGURATION;

    private final Map<Sha1, TrackedWorld> trackedWorlds;

    public MchConfiguration() {
        this.trackedWorlds = new HashMap<>();
    }

    public MchConfiguration(DataInput dataInput) throws IOException {
        FileMagic.validate(dataInput, MAGIC);
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 6);
        int size = dataInput.readInt();
        this.trackedWorlds = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            TrackedWorld trackedWorld = new TrackedWorld(dataInput);
            this.trackedWorlds.put(trackedWorld.getId(), trackedWorld);
        }
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MAGIC);
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeInt(this.trackedWorlds.size());
        for (TrackedWorld trackedWorld : this.trackedWorlds.values()) {
            trackedWorld.write(dataOutput);
        }
    }

    public Collection<TrackedWorld> getTrackedWorlds() {
        return this.trackedWorlds.values();
    }

    public TrackedWorld getTrackedWorld(Sha1 id) {
        return this.trackedWorlds.get(id);
    }

    public void trackWorld(TrackedWorld trackedWorld) {
        this.trackedWorlds.put(trackedWorld.getId(), trackedWorld);
    }
}
