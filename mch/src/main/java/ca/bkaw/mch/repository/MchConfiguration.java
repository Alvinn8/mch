package ca.bkaw.mch.repository;

import java.util.List;

public class MchConfiguration {
    private final List<TrackedWorld> trackedWorlds;

    public MchConfiguration(List<TrackedWorld> trackedWorlds) {
        this.trackedWorlds = trackedWorlds;
    }

    public List<TrackedWorld> getTrackedWorlds() {
        return this.trackedWorlds;
    }
}
