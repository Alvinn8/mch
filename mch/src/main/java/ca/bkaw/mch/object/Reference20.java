package ca.bkaw.mch.object;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.repository.MchRepository;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A 20-byte reference, aka an object being referenced by its SHA-1 hash.
 */
public class Reference20<T extends StorageObject> {
    private final ObjectStorageType<T> type;
    private final Sha1 ref;

    public Reference20(ObjectStorageType<T> type, Sha1 ref) {
        this.type = type;
        this.ref = ref;
    }

    public static <T extends StorageObject> Reference20<T> read(DataInput dataInput, ObjectStorageType<T> type) throws IOException {
        byte[] bytes = new byte[20];
        dataInput.readFully(bytes);
        return new Reference20<>(type, new Sha1(bytes));
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.write(this.ref.getBytes());
    }

    /**
     * Resolve this reference by reading the object from the object storage type.
     *
     * @param repository The repository to read from.
     * @return The read storage object.
     * @throws IOException If an I/O error occurs.
     * @see ObjectStorageType#read(Sha1, MchRepository)
     */
    public T resolve(MchRepository repository) throws IOException {
        return this.type.read(this.ref, repository);
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
