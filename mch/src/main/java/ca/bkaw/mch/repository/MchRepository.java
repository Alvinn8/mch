package ca.bkaw.mch.repository;

import ca.bkaw.mch.object.ObjectStorageType;
import ca.bkaw.mch.object.ObjectStorageTypes;

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
        try (DataInputStream stream = new DataInputStream(Files.newInputStream(path))) {
            this.configuration = new MchConfiguration(stream);
        }
    }

    public void saveConfiguration() throws IOException {
        Path path = this.getConfigurationPath();
        try (DataOutputStream stream = new DataOutputStream(Files.newOutputStream(path))) {
            this.configuration.write(stream);
        }
    }

    /**
     * Get the path to the root of the repository.
     *
     * @return The path.
     */
    public Path getRoot() {
        return this.root;
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
