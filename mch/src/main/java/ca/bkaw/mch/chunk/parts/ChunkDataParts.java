package ca.bkaw.mch.chunk.parts;

import java.io.DataInput;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A registry for the {@link ChunkDataPart}s that exist.
 */
public class ChunkDataParts {
    private static final Map<Byte, ChunkDataPart> BY_ID = new HashMap<>();

    /**
     * The {@link ChunkDataPart} that stores the remaining chunk data that was not
     * extracted by other chunk data parts. This {@link ChunkDataPart} must run last.
     */
    public static final ChunkDataPart REMAINING = register(new RemainingChunkDataPart(1));
    /**
     * Tags that update frequently.
     */
    public static final ChunkDataPart FREQUENT_UPDATERS = register(new SimpleChunkDataPart(2, Set.of("LastUpdate", "InhabitedTime")));
    /**
     * The "sections" tag.
     */
    public static final ChunkDataPart SECTIONS = register(new SectionChunkDataPart(3));
    /**
     * Block entities.
     */
    public static final ChunkDataPart BLOCK_ENTITIES = register(new SimpleChunkDataPart(4, Set.of("block_entities")));
    /**
     * Heightmaps.
     */
    public static final ChunkDataPart HEIGHTMAPS = register(new SimpleChunkDataPart(5, Set.of("Heightmaps")));

    /**
     * The sequence of {@link ChunkDataPart}s that the current version of mch uses.
     * <p>
     * The sequence makes little difference, but the important thing is that the
     * {@link #REMAINING} part is last.
     */
    public static final List<ChunkDataPart> CHUNK_DATA_PARTS = List.of(
        FREQUENT_UPDATERS,
        SECTIONS,
        BLOCK_ENTITIES,
        HEIGHTMAPS,
        REMAINING
    );

    private ChunkDataParts() {}

    private static ChunkDataPart register(ChunkDataPart chunkDataPart) {
        BY_ID.put(chunkDataPart.getId(), chunkDataPart);
        return chunkDataPart;
    }

    public static ChunkDataPart byId(byte id) {
        ChunkDataPart chunkDataPart = BY_ID.get(id);
        if (chunkDataPart == null) {
            throw new IllegalArgumentException("Unknown chunk data part id: " + id);
        }
        return chunkDataPart;
    }

    /**
     * @see RemainingChunkDataPartStorage
     */
    public static class RemainingChunkDataPart extends ChunkDataPart {
        public RemainingChunkDataPart(int id) {
            super((byte) id);
        }

        @Override
        public ChunkDataPartStorage createStorage() {
            return new RemainingChunkDataPartStorage();
        }

        @Override
        public ChunkDataPartStorage readStorage(DataInput dataInput) throws IOException {
            return new RemainingChunkDataPartStorage(dataInput);
        }
    }

    /**
     * @see SimpleChunkDataPartStorage
     */
    public static class SimpleChunkDataPart extends ChunkDataPart {
        private final Set<String> keys;

        public SimpleChunkDataPart(int id, Set<String> keys) {
            super((byte) id);
            this.keys = keys;
        }

        @Override
        public ChunkDataPartStorage createStorage() {
            return new SimpleChunkDataPartStorage(this.keys);
        }

        @Override
        public ChunkDataPartStorage readStorage(DataInput dataInput) throws IOException {
            return new SimpleChunkDataPartStorage(this.keys, dataInput);
        }
    }

    /**
     * @see SimpleChunkDataPartStorage
     */
    public static class SectionChunkDataPart extends ChunkDataPart {
        public SectionChunkDataPart(int id) {
            super((byte) id);
        }

        @Override
        public ChunkDataPartStorage createStorage() {
            return new SectionChunkDataPartStorage();
        }

        @Override
        public ChunkDataPartStorage readStorage(DataInput dataInput) throws IOException {
            return new SectionChunkDataPartStorage(dataInput);
        }
    }
}
