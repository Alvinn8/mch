package ca.bkaw.mch.object;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.repository.MchRepository;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A 20-byte reference, aka an object being referenced by its SHA-1 hash.
 */
public class Reference20 {
    private final Sha1 ref;

    public Reference20(Sha1 ref) {
        this.ref = ref;
    }

    public static Reference20 read(DataInputStream stream) throws IOException {
        byte[] bytes = new byte[20];
        stream.readFully(bytes);
        return new Reference20(new Sha1(bytes));
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.write(this.ref.getBytes());
    }

    /**
     * Resolve this reference by reading the object from the provided object storage
     * type.
     *
     * @param type The object storage type.
     * @param repository The repository to read from.
     * @return The read storage object.
     * @param <T> The type of object.
     * @throws IOException If an I/O error occurs.
     * @see ObjectStorageType#read(Sha1, MchRepository)
     */
    public <T extends StorageObject> T resolve(ObjectStorageType<T> type, MchRepository repository) throws IOException {
        return type.read(this.ref, repository);
    }

    /**
     * Get the SHA-1 hash of the object referenced by this reference.
     *
     * @return The SHA-1 hash.
     */
    public Sha1 getSha1() {
        return this.ref;
    }
}
