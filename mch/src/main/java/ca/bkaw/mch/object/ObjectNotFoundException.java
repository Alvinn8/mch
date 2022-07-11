package ca.bkaw.mch.object;

/**
 * Thrown when a {@link StorageObject} was not found.
 */
public class ObjectNotFoundException extends RuntimeException {
    public ObjectNotFoundException(String hex, String typeId) {
        super("The object \"" + hex + "\" can not be found in object storage type \"" + typeId + "\".");
    }
}
