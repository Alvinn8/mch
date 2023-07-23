package ca.bkaw.mch.repository;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.world.WorldProvider;

public class TrackedWorld {
    private final Sha1 id;
    private final WorldProvider worldProvider;

    public TrackedWorld(Sha1 id, WorldProvider worldProvider) {
        this.id = id;
        this.worldProvider = worldProvider;
    }

    public Sha1 getId() {
        return this.id;
    }

    public WorldProvider getWorldProvider() {
        return this.worldProvider;
    }
}
