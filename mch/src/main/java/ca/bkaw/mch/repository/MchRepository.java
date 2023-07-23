package ca.bkaw.mch.repository;

import ca.bkaw.mch.object.ObjectStorageType;
import ca.bkaw.mch.object.ObjectStorageTypes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class MchRepository {
    /**
     * The root of the repository.
     */
    private final Path root;
    private MchConfiguration configuration = new MchConfiguration(new ArrayList<>());

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

    /**
     * Get the path to the root of the repository.
     *
     * @return The path.
     */
    public Path getRoot() {
        return this.root;
    }

    public MchConfiguration getConfiguration() {
        return this.configuration;
    }
}
