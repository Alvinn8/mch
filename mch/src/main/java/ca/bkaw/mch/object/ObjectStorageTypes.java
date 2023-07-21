package ca.bkaw.mch.object;

import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.dimension.Dimension;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The different types of objects that can be stored in object storage.
 */
public class ObjectStorageTypes {
    /**
     * A list of all object storage types.
     */
    private static final Map<String, ObjectStorageType<?>> BY_ID = new HashMap<>();

    /**
     * Stores a commit.
     * <p>
     * Commits reference dimensions.
     */
    public static final ObjectStorageType<Commit> COMMIT
        = new ObjectStorageType<>("commit", Commit::new);

    /**
     * Stores a dimension of a world.
     * <p>
     * Dimensions reference region files.
     */
    public static final ObjectStorageType<Dimension> DIMENSION
        = new ObjectStorageType<>("dimension", Dimension::new);

    /**
     * Get an object storage type by id.
     *
     * @param id The id of the object storage type.
     * @return The object storage type, or null.
     */
    @Nullable
    public static ObjectStorageType<?> getType(String id) {
        return BY_ID.get(id);
    }

    public static Iterable<ObjectStorageType<?>> values() {
        return BY_ID.values();
    }

    private static <T extends StorageObject> ObjectStorageType<T> register(ObjectStorageType<T> objectStorageType) {
        BY_ID.put(objectStorageType.getId(), objectStorageType);
        return objectStorageType;
    }
}
