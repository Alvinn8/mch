package ca.bkaw.mch.object;

import ca.bkaw.mch.object.chunk.Chunk;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.regionfile.RegionFile;

import java.util.ArrayList;
import java.util.List;

/**
 * The different types of objects that can be stored in object storage.
 */
public class ObjectStorageTypes {
    /**
     * A list of all object storage types.
     */
    public static final List<ObjectStorageType<?>> VALUES = new ArrayList<>();

    /**
     * Get an object storage type by id.
     *
     * @param id The id of the object storage type.
     * @return The object storage type, or null.
     */
    public static ObjectStorageType<?> getType(String id) {
        for (ObjectStorageType<?> type : VALUES) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }

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
     * Stores a region file in a dimension.
     * <p>
     * Region files reference chunks.
     */
    public static final ObjectStorageType<RegionFile> REGION_FILE
        = new ObjectStorageType<>("region_file", RegionFile::new);

    /**
     * Stores a chunk in a region file.
     * <p>
     * Chunks reference chunk sections and chunk nbt.
     */
    public static final ObjectStorageType<Chunk> CHUNK
        = new ObjectStorageType<>("chunk", Chunk::new);
}
