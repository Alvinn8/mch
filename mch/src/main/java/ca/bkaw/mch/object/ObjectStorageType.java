package ca.bkaw.mch.object;

import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.repository.MchRepository;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class ObjectStorageType<T extends StorageObject> {
    public static final String OBJECT_STORAGE_FOLDER = "objects";

    private final String id;
    private final StorageObjectConstructor<T> constructor;

    public ObjectStorageType(String id, StorageObjectConstructor<T> constructor) {
        this.id = id;
        this.constructor = constructor;

        ObjectStorageTypes.VALUES.add(this);
    }

    /**
     * Get the path where objects are stored in the repository for this object
     * storage type.
     *
     * @param repository The repository.
     * @return The object directory.
     */
    private Path getObjectsPath(MchRepository repository) {
        return repository.getRoot()
            .resolve(OBJECT_STORAGE_FOLDER)
            .resolve(this.id)
            .resolve(OBJECT_STORAGE_FOLDER);
    }


    /**
     * Create the directories in the repository where objects will be stored in this
     * object storage type.
     *
     * @param repository The repository.
     * @throws IOException If an I/O error occurs.
     */
    public void createDirectories(MchRepository repository) throws IOException {
        Files.createDirectories(this.getObjectsPath(repository));
    }

    /**
     * Save a storage object.
     *
     * @param storageObject The object to save.
     * @param repository The repository to save to.
     * @return The identifying SHA-1 hash the object was stored as.
     * @throws IOException If an I/O error occurs.
     */
    public Sha1 save(T storageObject, MchRepository repository) throws IOException {
        Path objectsPath = this.getObjectsPath(repository);
        Path tempFile = Files.createTempFile(objectsPath, null, null);
        try (DataOutputStream stream = new DataOutputStream(Files.newOutputStream(tempFile))) {
            stream.writeInt(MchVersion.VERSION_NUMBER);
            storageObject.serialize(stream);
        }

        Sha1 hash = storageObject.getSha1Hash(tempFile);
        String hex = hash.asHex();
        String group = hex.substring(0, 2);

        Path zipPath = objectsPath.resolve(group + ".zip");

        Map<String, Object> env = new HashMap<>();
        if (Files.notExists(zipPath)) {
            env.put("create", true);
        }
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()), env)) {
            Path path = fs.getPath(hex);
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        }
        return hash;
    }

    /**
     * Read a storage object from its identifying SHA-1 hash.
     *
     * @param sha1 The SHA-1 hash that identifies the object to read.
     * @param repository The repository to read from.
     * @return The read storage object.
     * @throws ObjectNotFoundException If the object was not found.
     * @throws IOException If an I/O error occurs.
     */
    public T read(Sha1 sha1, MchRepository repository) throws IOException {
        String hex = sha1.asHex();
        String group = hex.substring(0, 2);

        Path zipPath = this.getObjectsPath(repository).resolve(group + ".zip");
        if (Files.notExists(zipPath)) {
            throw new ObjectNotFoundException(hex, this.id);
        }

        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()), new HashMap<>())) {
            Path path = fs.getPath(hex);
            if (Files.notExists(path)) {
                throw new ObjectNotFoundException(hex, this.id);
            }
            DataInputStream stream = new DataInputStream(Files.newInputStream(path));
            int version = stream.readInt();
            if (version != MchVersion.VERSION_NUMBER) {
                throw new IllegalStateException("Unsupported version " + version + ", current is " + MchVersion.VERSION_NUMBER);
            }
            return this.constructor.create(stream);
        }
    }

    /**
     * Check if an object exists with the specified identifying SHA-1 hash.
     *
     * @param sha1 The SHA-1 hash.
     * @param repository The repository to read from.
     * @return Whether the object exists.
     * @throws IOException If an I/O error occurs.
     */
    public boolean exists(Sha1 sha1, MchRepository repository) throws IOException {
        String hex = sha1.asHex();
        String group = hex.substring(0, 2);

        Path zipPath = this.getObjectsPath(repository).resolve(group + ".zip");
        if (Files.notExists(zipPath)) {
            return false;
        }

        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()), new HashMap<>())) {
            Path path = fs.getPath(hex);
            return Files.exists(path);
        }
    }

    public String getId() {
        return this.id;
    }
}
