package ca.bkaw.mch.chunk;

import ca.bkaw.mch.nbt.NbtCompound;

/**
 * An object representing the chunk saved in a region file.
 * <p>
 * In addition to the chunk nbt, the last modified time stored in the region file
 * header is stored in this object.
 *
 * @param nbt The chunk nbt.
 * @param lastModified The last modified time of the chunk, in epoch seconds.
 */
public record RegionFileChunk(NbtCompound nbt, int lastModified) {
}
