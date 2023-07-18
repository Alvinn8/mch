package ca.bkaw.mch.chunk.parts;

import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.UnsupportedMchVersionException;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChunkDataPartStorage {
    private final Map<Integer, NbtCompound> dataParts;
    private final Map<NbtCompound, Integer> reverse;

    public ChunkDataPartStorage(DataInput dataInput) throws IOException {
        int mchVersion = dataInput.readInt();
        if (mchVersion != MchVersion.VERSION_NUMBER) {
            throw new UnsupportedMchVersionException("Unsupported mch version "+ mchVersion +" when reading chunk data part storage. Only " + MchVersion.VERSION_NUMBER + " is supported.");
        }
        int size = dataInput.readInt();
        this.dataParts = new LinkedHashMap<>(size);
        this.reverse = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            int versionNumber = dataInput.readInt();
            NbtCompound nbt = NbtTag.readCompound(dataInput);
            this.set(versionNumber, nbt);
        }
    }

    public ChunkDataPartStorage() {
        this.dataParts = new LinkedHashMap<>();
        this.reverse = new HashMap<>();
    }

    private void set(int versionNumber, NbtCompound dataPartNbt) {
        this.dataParts.put(versionNumber, dataPartNbt);
        this.reverse.put(dataPartNbt, versionNumber);
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeInt(this.dataParts.size());
        for (Map.Entry<Integer, NbtCompound> entry : this.dataParts.entrySet()) {
            int versionNumber = entry.getKey();
            NbtCompound dataPartNbt = entry.getValue();
            dataOutput.writeInt(versionNumber);
            NbtTag.writeTag(dataOutput, dataPartNbt);
        }
    }

    public int store(@NotNull NbtCompound dataPartNbt) {
        Integer existingVersionNumber = this.reverse.get(dataPartNbt);
        if (existingVersionNumber != null) {
            return existingVersionNumber;
        }
        int versionNumber = 1;
        while (this.dataParts.containsKey(versionNumber)) {
            versionNumber++;
        }
        this.set(versionNumber, dataPartNbt);
        return versionNumber;
    }

    @NotNull
    public NbtCompound get(int versionNumber) {
        NbtCompound nbt = this.dataParts.get(versionNumber);
        if (nbt == null) {
            throw new RuntimeException("Unable to find data part with version number " + versionNumber + " in the data part storage.");
        }
        return nbt;
    }
}
