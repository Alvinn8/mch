package ca.bkaw.mch.repository;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.ObjectStorageType;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MchRepository {
    /**
     * The root of the repository.
     */
    private final Path root;
    private MchConfiguration configuration;

    public MchRepository(Path root) {
        this.root = root;
    }

    /**
     * Create the directories needed for this repository.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void createDirectories() throws IOException {
        for (ObjectStorageType<?> objectStorageType : ObjectStorageTypes.values()) {
            objectStorageType.createDirectories(this);
        }
    }

    private Path getConfigurationPath() {
        return this.root.resolve("configuration");
    }

    public void readConfiguration() throws IOException {
        Path path = this.getConfigurationPath();
        if (Files.notExists(path)) {
            this.configuration = new MchConfiguration();
            return;
        }
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            this.configuration = new MchConfiguration(stream);
        }
    }

    public void saveConfiguration() throws IOException {
        Path path = this.getConfigurationPath();
        try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
            this.configuration.write(stream);
        }
    }

    private Path getHeadPath() {
        return this.root.resolve("head");
    }

    /**
     * Get the head commit, the latest commit.
     *
     * @return The head commit.
     * @throws IOException If an I/O error occurs.
     */
    @Nullable
    public Reference20<Commit> getHeadCommit() throws IOException {
        Path path = this.getHeadPath();
        if (Files.notExists(path)) {
            return null;
        }
        String str = Files.readString(path).trim();
        if (str.isEmpty()) {
            return null;
        }
        Sha1 sha1 = Sha1.fromString(str);
        return new Reference20<>(ObjectStorageTypes.COMMIT, sha1);
    }

    /**
     * Set the head commit, the latest commit.
     *
     * @param commitReference The reference to the commit, or null.
     * @throws IOException If an I/O error occurs.
     */
    public void setHeadCommit(@Nullable Reference20<Commit> commitReference) throws IOException {
        Path path = this.getHeadPath();
        String str = commitReference == null ? "" : commitReference.getSha1().asHex();
        Files.writeString(path, str);
    }

    /**
     * Get the path to the root of the repository. Also known as the "mch" directory.
     *
     * @return The path.
     */
    public Path getRoot() {
        return this.root;
    }

    /**
     * Get the directory where the user initialized the repository.
     * <p>
     * This is the directory that contains the {@code mch} directory as gotten by
     * {@link #getRoot()}.
     *
     * @return The project directory path.
     */
    public Path getProjectDirectory() {
        return this.root.getParent();
    }

    /**
     * Get the {@link MchConfiguration configuration}.
     *
     * @return The configuration.
     */
    public MchConfiguration getConfiguration() {
        if (this.configuration == null) {
            throw new IllegalStateException("Configuration has not been read.");
        }
        return this.configuration;
    }
}
