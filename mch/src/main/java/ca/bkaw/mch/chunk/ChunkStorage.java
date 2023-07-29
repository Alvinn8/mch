package ca.bkaw.mch.chunk;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.chunk.parts.ChunkDataPart;
import ca.bkaw.mch.chunk.parts.ChunkDataPartStorage;
import ca.bkaw.mch.chunk.parts.ChunkDataParts;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.util.BiMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Storage of different versions of a chunk.
 */
public class ChunkStorage {
    public static final int MAGIC = FileMagic.CHUNK_STORAGE;
    
    private final BiMap<Integer, MchChunk> chunkVersions;
    private final Map<Byte, ChunkDataPartStorage> chunkPartStorage;

    public ChunkStorage(DataInput dataInput) throws IOException {
        FileMagic.validate(dataInput, MAGIC);
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 2);
        int chunkVersionsSize = dataInput.readInt();
        this.chunkVersions = new BiMap<>(chunkVersionsSize);
        for (int i = 0; i < chunkVersionsSize; i++) {
            int chunkVersionNumber = dataInput.readInt();
            MchChunk mchChunk = new MchChunk(dataInput);
            this.chunkVersions.put(chunkVersionNumber, mchChunk);
        }
        int chunkPartStorageSize = dataInput.readInt();
        this.chunkPartStorage = new HashMap<>(chunkPartStorageSize);
        for (int i = 0; i < chunkPartStorageSize; i++) {
            byte dataPartId = dataInput.readByte();
            ChunkDataPart dataPart = ChunkDataParts.byId(dataPartId);
            ChunkDataPartStorage dataPartStorage = dataPart.readStorage(dataInput);
            this.chunkPartStorage.put(dataPartId, dataPartStorage);
        }
    }

    public ChunkStorage() {
        this.chunkVersions = new BiMap<>();
        this.chunkPartStorage = new HashMap<>();
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
        for (Map.Entry<Byte, ChunkDataPartStorage> entry : this.chunkPartStorage.entrySet()) {
            dataOutput.writeByte(entry.getKey());
            entry.getValue().write(dataOutput);
        }
    }

    public int store(NbtCompound chunk) {
        MchChunk mchChunk = new MchChunk();

        // Split the chunk nbt into parts
        for (ChunkDataPart chunkDataPart : ChunkDataParts.CHUNK_DATA_PARTS) {
            // Get the storage for this data part
            ChunkDataPartStorage partStorage = this.chunkPartStorage.get(chunkDataPart.getId());
            if (partStorage == null) {
                partStorage = chunkDataPart.createStorage();
                this.chunkPartStorage.put(chunkDataPart.getId(), partStorage);
            }

            // Store the part
            int dataPartVersionNumber = partStorage.storePart(chunk);

            // Reference the version number of the data part in the MchChunk
            mchChunk.setNbtPartVersionNumber(chunkDataPart.getId(), dataPartVersionNumber);
        }

        // Check if the newly created MchChunk instance is equal to an already existing
        // chunk in the storage. In that case we reuse the existing object instead of
        // storing it again and return the version number of the existing chunk.
        Integer existingVersionNumber = this.chunkVersions.reverse().get(mchChunk);
        if (existingVersionNumber != null) {
            return existingVersionNumber;
        }

        // Find an unoccupied version number.
        int chunkVersionNumber = 1;
        while (this.chunkVersions.containsKey(chunkVersionNumber)) {
            chunkVersionNumber++;
        }

        this.chunkVersions.put(chunkVersionNumber, mchChunk);

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
            ChunkDataPartStorage partStorage = this.chunkPartStorage.get(dataPartId);
            if (partStorage == null) {
                throw new RuntimeException("Chunk requested data part id " + dataPartId + " but no storage for that data part existed.");
            }

            // Restore this part of the chunk nbt
            partStorage.restorePart(chunkNbt, dataPartVersionNumber);
        }
        return chunkNbt;
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
