package ca.bkaw.mch.repository;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.world.WorldAccessor;
import ca.bkaw.mch.world.ftp.FtpProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MchConfiguration {
    public static final int MAGIC = FileMagic.CONFIGURATION;

    private final List<TrackedWorld> trackedWorlds;
    private final Map<String, FtpProfile> ftpProfiles;

    public MchConfiguration() {
        this.trackedWorlds = new ArrayList<>();
        this.ftpProfiles = new HashMap<>();
    }

    public MchConfiguration(DataInput dataInput) throws IOException {
        FileMagic.validate(dataInput, MAGIC);
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 6);

        int trackedWorldsSize = dataInput.readInt();
        this.trackedWorlds = new ArrayList<>(trackedWorldsSize);
        for (int i = 0; i < trackedWorldsSize; i++) {
            TrackedWorld trackedWorld = new TrackedWorld(dataInput);
            this.trackedWorlds.add(trackedWorld);
        }

        int ftpProfilesSize = mchVersion > 9 ? dataInput.readInt() : 0;
        this.ftpProfiles = new HashMap<>(ftpProfilesSize);
        for (int i = 0; i < ftpProfilesSize; i++) {
            String profileName = dataInput.readUTF();
            FtpProfile ftpProfile = new FtpProfile(dataInput);
            this.ftpProfiles.put(profileName, ftpProfile);
        }

    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MAGIC);
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeInt(this.trackedWorlds.size());
        for (TrackedWorld trackedWorld : this.trackedWorlds) {
            trackedWorld.write(dataOutput);
        }
        dataOutput.writeInt(this.ftpProfiles.size());
        for (Map.Entry<String, FtpProfile> entry : this.ftpProfiles.entrySet()) {
            dataOutput.writeUTF(entry.getKey());
            entry.getValue().write(dataOutput);
        }
    }

    public Collection<TrackedWorld> getTrackedWorlds() {
        return Collections.unmodifiableCollection(this.trackedWorlds);
    }

    /**
     * Get a tracked world by id, and throw an exception if it does not exist.
     *
     * @param id The world id.
     * @return The tracked world.
     */
    @NotNull
    public TrackedWorld getTrackedWorld(Sha1 id) {
        for (TrackedWorld trackedWorld : this.trackedWorlds) {
            if (trackedWorld.getId().equals(id)) {
                return trackedWorld;
            }
        }
        throw new RuntimeException("The provided world id " + id + " is not tracked in this repository.");
    }

    /**
     * Get a tracked world by name, or null if no world with that name was found.
     *
     * @param name The name of the world.
     * @return The tracked world.
     */
    @Nullable
    public TrackedWorld getTrackedWorld(String name) {
        for (TrackedWorld trackedWorld : this.trackedWorlds) {
            if (name.equals(trackedWorld.getName())) {
                return trackedWorld;
            }
        }
        return null;
    }

    /**
     * Track a new world.
     * <p>
     * The world's name will be modified in case the name is already occupied.
     *
     * @param trackedWorld The world to track.
     */
    public void trackWorld(TrackedWorld trackedWorld) {
        int i = 1;
        String originalName = trackedWorld.getName();
        while (this.getTrackedWorld(trackedWorld.getName()) != null) {
            trackedWorld.setName(originalName + "-" + i);
        }
        this.trackedWorlds.add(trackedWorld);
    }

    /**
     * Check if a world is already being tracked.
     *
     * @param worldAccessor The world accessor.
     * @return Whether an equal world accessor is already used somewhere.
     */
    public boolean alreadyTracking(WorldAccessor worldAccessor) {
        for (TrackedWorld trackedWorld : this.trackedWorlds) {
            if (trackedWorld.getWorldAccessor().equals(worldAccessor)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, FtpProfile> getFtpProfiles() {
        return Collections.unmodifiableMap(this.ftpProfiles);
    }

    /**
     * Get an FTP profile by name.
     *
     * @param name The name of the ftp profile.
     * @return The ftp profile, or null.
     */
    @Nullable
    public FtpProfile getFtpProfile(String name) {
        return this.ftpProfiles.get(name);
    }

    /**
     * Set an FTP profile by name.
     *
     * @param name The name.
     * @param ftpProfile The FTP profile.
     */
    public void setFtpProfile(String name, @Nullable FtpProfile ftpProfile) {
        this.ftpProfiles.put(name, ftpProfile);
    }

}
