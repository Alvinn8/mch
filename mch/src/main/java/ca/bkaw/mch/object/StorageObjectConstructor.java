package ca.bkaw.mch.object;

import java.io.DataInputStream;
import java.io.IOException;

@FunctionalInterface
public interface StorageObjectConstructor<T extends StorageObject> {
    /**
     * Create a storage object from bytes.
     *
     * @param stream The stream to read from.
     * @return The created storage object.
     * @throws IOException If an I/O error occurs.
     */
    T create(DataInputStream stream) throws IOException;
}
