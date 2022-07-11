package ca.bkaw.mch.object;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.repository.MchRepository;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * An object that is stored by its SHA-1 hash on disk.
 */
public abstract class StorageObject {
    /**
     * Save this object to bytes.
     *
     * @param stream The stream to write to.
     * @see ObjectStorageType#save(StorageObject, MchRepository)
     */
    public abstract void serialize(DataOutputStream stream) throws IOException;

    /**
     * Print this storage object as a human readable string.
     * <p>
     * This will be used for the {@code mch cat-file} command output.
     *
     * @return The string.
     */
    public abstract String cat();

    /**
     * Get the SHA-1 hash for identifying this object.
     *
     * @param tempFile The temporary file containing the serialized object.
     * @return The SHA-1 hash.
     */
    public Sha1 getSha1Hash(Path tempFile) throws IOException {
        return Sha1.ofFile(tempFile);
    }
}
