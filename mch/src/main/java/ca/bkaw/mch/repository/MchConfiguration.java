package ca.bkaw.mch.repository;

import ca.bkaw.mch.FileMagic;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MchConfiguration {
    public static final int MAGIC = FileMagic.CONFIGURATION;

    private final List<TrackedWorld> trackedWorlds;

    public MchConfiguration() {
        this.trackedWorlds = new ArrayList<>();
    }

    public MchConfiguration(DataInput dataInput) throws IOException {
        FileMagic.validate(dataInput, MAGIC);
        int size = dataInput.readInt();
        this.trackedWorlds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.trackedWorlds.add(new TrackedWorld(dataInput));
        }
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MAGIC);
        dataOutput.writeInt(this.trackedWorlds.size());
        for (TrackedWorld trackedWorld : this.trackedWorlds) {
            trackedWorld.write(dataOutput);
        }
    }

    public List<TrackedWorld> getTrackedWorlds() {
        return this.trackedWorlds;
    }
}
