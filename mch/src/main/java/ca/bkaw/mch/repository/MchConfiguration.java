package ca.bkaw.mch.repository;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.world.WorldAccessor;
import ca.bkaw.mch.world.ftp.FtpProfile;
import ca.bkaw.mch.world.sftp.SftpProfile;
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
    private final Profiles<FtpProfile> ftpProfiles;
    private final Profiles<SftpProfile> sftpProfiles;

    public MchConfiguration() {
        this.trackedWorlds = new ArrayList<>();
        this.ftpProfiles = new Profiles<>();
        this.sftpProfiles = new Profiles<>();
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
        this.ftpProfiles = new Profiles<>(new HashMap<>(ftpProfilesSize));
        for (int i = 0; i < ftpProfilesSize; i++) {
            String profileName = dataInput.readUTF();
            FtpProfile ftpProfile = new FtpProfile(dataInput);
            this.ftpProfiles.setProfile(profileName, ftpProfile);
        }

        int sftpProfilesSize = mchVersion > 12 ? dataInput.readInt() : 0;
        this.sftpProfiles = new Profiles<>(new HashMap<>(sftpProfilesSize));
        for (int i = 0; i < sftpProfilesSize; i++) {
            String profileName = dataInput.readUTF();
            SftpProfile sftpProfile = new SftpProfile(dataInput);
            this.sftpProfiles.setProfile(profileName, sftpProfile);
        }
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MAGIC);
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeInt(this.trackedWorlds.size());
        for (TrackedWorld trackedWorld : this.trackedWorlds) {
            trackedWorld.write(dataOutput);
        }
        Map<String, FtpProfile> ftpProfiles = this.ftpProfiles.getProfiles();
        dataOutput.writeInt(ftpProfiles.size());
        for (Map.Entry<String, FtpProfile> entry : ftpProfiles.entrySet()) {
            dataOutput.writeUTF(entry.getKey());
            entry.getValue().write(dataOutput);
        }
        Map<String, SftpProfile> sftpProfiles = this.sftpProfiles.getProfiles();
        dataOutput.writeInt(sftpProfiles.size());
        for (Map.Entry<String, SftpProfile> entry : sftpProfiles.entrySet()) {
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

    /**
     * Get the FTP profiles.
     *
     * @return The FTP profiles.
     */
    public Profiles<FtpProfile> getFtpProfiles() {
        return this.ftpProfiles;
    }

    /**
     * Get the SFTP profiles.
     *
     * @return The SFTP profiles.
     */
    public Profiles<SftpProfile> getSftpProfiles() {
        return this.sftpProfiles;
    }
}
