package ca.bkaw.mch.chunk;

import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.NbtPartStorage;
import ca.bkaw.mch.chunk.parts.ChunkDataPart;
import ca.bkaw.mch.chunk.parts.ChunkDataParts;
import ca.bkaw.mch.nbt.NbtCompound;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Storage of different versions of a chunk.
 */
public class MchChunkStorage {
    public static final int MAGIC = 0x4D434821;
    
    private final Map<Integer, MchChunk> chunkVersions;
    private final Map<MchChunk, Integer> chunkVersionsReverse;
    private final Map<Byte, NbtPartStorage> chunkPartStorage;

    public MchChunkStorage(DataInput dataInput) throws IOException {
        if (dataInput.readInt() != MAGIC) {
            throw new RuntimeException("Expected mch chunk storage magic header. Is the mch chunk file corrupted?");
        }
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 2);
        int chunkVersionsSize = dataInput.readInt();
        this.chunkVersions = new HashMap<>(chunkVersionsSize);
        this.chunkVersionsReverse = new HashMap<>(chunkVersionsSize);
        for (int i = 0; i < chunkVersionsSize; i++) {
            int chunkVersionNumber = dataInput.readInt();
            MchChunk mchChunk = new MchChunk(dataInput);
            this.set(chunkVersionNumber, mchChunk);
        }
        int chunkPartStorageSize = dataInput.readInt();
        this.chunkPartStorage = new HashMap<>(chunkPartStorageSize);
        for (int i = 0; i < chunkPartStorageSize; i++) {
            byte dataPartId = dataInput.readByte();
            NbtPartStorage nbtPartStorage = new NbtPartStorage(dataInput);
            this.chunkPartStorage.put(dataPartId, nbtPartStorage);
        }
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MAGIC);
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeInt(this.chunkVersions.size());
        for (Map.Entry<Integer, MchChunk> entry : this.chunkVersions.entrySet()) {
            dataOutput.writeInt(entry.getKey());
            entry.getValue().write(dataOutput);
        }
        dataOutput.writeInt(this.chunkPartStorage.size());
        for (Map.Entry<Byte, NbtPartStorage> entry : this.chunkPartStorage.entrySet()) {
            dataOutput.writeByte(entry.getKey());
            entry.getValue().write(dataOutput);
        }
    }

    public MchChunkStorage() {
        this.chunkVersions = new HashMap<>();
        this.chunkVersionsReverse = new HashMap<>();
        this.chunkPartStorage = new HashMap<>();
    }

    private void set(int chunkVersionNumber, MchChunk mchChunk) {
        this.chunkVersions.put(chunkVersionNumber, mchChunk);
        this.chunkVersionsReverse.put(mchChunk, chunkVersionNumber);
    }

    public int store(NbtCompound chunk) {
        MchChunk mchChunk = new MchChunk();

        // Split the chunk nbt into parts
        for (ChunkDataPart chunkDataPart : ChunkDataParts.CHUNK_DATA_PARTS) {
            NbtCompound dataPartNbt = chunkDataPart.extract(chunk);

            // Get the storage for this data part
            NbtPartStorage storage = this.chunkPartStorage.get(chunkDataPart.getId());
            if (storage == null) {
                storage = new NbtPartStorage();
                this.chunkPartStorage.put(chunkDataPart.getId(), storage);
            }

            // Store the data part
            int dataPartVersionNumber = storage.store(dataPartNbt);

            // Reference the version number of the data part in the MchChunk
            mchChunk.setNbtPartVersionNumber(chunkDataPart.getId(), dataPartVersionNumber);
        }

        // Check if the newly created MchChunk instance is equal to an already existing
        // chunk in the storage. In that case we reuse the existing object instead of
        // storing it again and return the version number of the existing chunk.
        Integer existingVersionNumber = this.chunkVersionsReverse.get(mchChunk);
        if (existingVersionNumber != null) {
            return existingVersionNumber;
        }

        // Find an unoccupied version number.
        int chunkVersionNumber = 1;
        while (this.chunkVersions.containsKey(chunkVersionNumber)) {
            chunkVersionNumber++;
        }

        this.set(chunkVersionNumber, mchChunk);

        return chunkVersionNumber;
    }

    public NbtCompound restore(int versionNumber) {
        MchChunk mchChunk = this.chunkVersions.get(versionNumber);
        if (mchChunk == null) {
            throw new RuntimeException("Chunk with version number " + versionNumber + " was not present in storage.");
        }
        // Merge the nbt that was split back into one compound
        NbtCompound chunkNbt = new NbtCompound();
        for (Map.Entry<Byte, Integer> entry : mchChunk.nbtPartVersionNumbers.entrySet()) {
            byte dataPartId = entry.getKey();
            int dataPartVersionNumber = entry.getValue();

            // Get the data part storage
            NbtPartStorage nbtPartStorage = this.chunkPartStorage.get(dataPartId);
            if (nbtPartStorage == null) {
                throw new RuntimeException("Chunk requested data part id " + dataPartId + " but no storage for that data part existed.");
            }

            // Get the data part from the data part storage
            NbtCompound nbtPart = nbtPartStorage.get(dataPartVersionNumber);

            // Merge it according to the data part
            ChunkDataPart chunkDataPart = ChunkDataParts.byId(dataPartId);
            chunkDataPart.merge(chunkNbt, nbtPart);
        }
        return chunkNbt;
    }

    public void test() {
        System.out.println("this.chunkVersions = " + this.chunkVersions);
        System.out.println("this.chunkPartStorage = " + this.chunkPartStorage);
    }

    public static class MchChunk {
        private final Map<Byte, Integer> nbtPartVersionNumbers;

        public MchChunk(DataInput dataInput) throws IOException {
            int size = dataInput.readInt();
            this.nbtPartVersionNumbers = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                byte dataPartId = dataInput.readByte();
                int versionNumber = dataInput.readInt();
                this.nbtPartVersionNumbers.put(dataPartId, versionNumber);
            }
        }

        public MchChunk() {
            this.nbtPartVersionNumbers = new HashMap<>();
        }

        public void write(DataOutput dataOutput) throws IOException {
            dataOutput.writeInt(this.nbtPartVersionNumbers.size());
            for (Map.Entry<Byte, Integer> entry : this.nbtPartVersionNumbers.entrySet()) {
                dataOutput.writeByte(entry.getKey());
                dataOutput.writeInt(entry.getValue());
            }
        }

        public void setNbtPartVersionNumber(byte dataPartId, int versionNumber) {
            this.nbtPartVersionNumbers.put(dataPartId, versionNumber);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MchChunk mchChunk = (MchChunk) o;

            return this.nbtPartVersionNumbers.equals(mchChunk.nbtPartVersionNumbers);
        }

        @Override
        public int hashCode() {
            return this.nbtPartVersionNumbers.hashCode();
        }

        @Override
        public String toString() {
            return "MchChunk" + this.nbtPartVersionNumbers;
        }
    }
}
