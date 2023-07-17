package ca.bkaw.mch.chunk.parts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ChunkDataPartsTests {
    @Test
    public void ensureRemainingIsLast() {
        ChunkDataPart last = ChunkDataParts.CHUNK_DATA_PARTS.get(ChunkDataParts.CHUNK_DATA_PARTS.size() - 1);
        assertInstanceOf(RemainingChunkDataPart.class, last);
        assertEquals(last.getId(), 1);
    }

    @Test
    public void ensureGettingPartsByIdWorks() {
        assertEquals(ChunkDataParts.byId(1), ChunkDataParts.REMAINING);
    }
}
