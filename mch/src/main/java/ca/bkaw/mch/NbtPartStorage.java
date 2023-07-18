package ca.bkaw.mch;

import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A storage that stores nbt tags, usually different versions of the same tag.
 * <p>
 * Each nbt tag is associated with a version number unique to the storage.
 */
public class NbtPartStorage {
    private final Map<Integer, NbtCompound> data;
    private final Map<NbtCompound, Integer> reverse;

    /**
     * Read a serialized {@link NbtPartStorage}.
     *
     * @param dataInput The data input to read from.
     * @throws IOException If an I/O error occurs.
     */
    public NbtPartStorage(DataInput dataInput) throws IOException {
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 2);
        int size = dataInput.readInt();
        this.data = new LinkedHashMap<>(size);
        this.reverse = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            int versionNumber = dataInput.readInt();
            NbtCompound nbt = NbtTag.readCompound(dataInput);
            this.set(versionNumber, nbt);
        }
    }

    /**
     * Create a new empty nbt part storage.
     */
    public NbtPartStorage() {
        this.data = new LinkedHashMap<>();
        this.reverse = new HashMap<>();
    }

    private void set(int versionNumber, NbtCompound nbt) {
        this.data.put(versionNumber, nbt);
        this.reverse.put(nbt, versionNumber);
    }

    /**
     * Write the storage.
     *
     * @param dataOutput The data output to write to.
     * @throws IOException If an I/O error occurs.
     */
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeInt(this.data.size());
        for (Map.Entry<Integer, NbtCompound> entry : this.data.entrySet()) {
            int versionNumber = entry.getKey();
            NbtCompound dataPartNbt = entry.getValue();
            dataOutput.writeInt(versionNumber);
            NbtTag.writeTag(dataOutput, dataPartNbt);
        }
    }

    /**
     * Store an nbt tag in this storage.
     * <p>
     * If an identical nbt tag is already stored in this storage, the nbt tag will not
     * be stored again and the existing version number will be returned.
     *
     * @param nbt The nbt tag to store.
     * @return The version number the nbt tag was stored as.
     */
    public int store(@NotNull NbtCompound nbt) {
        Integer existingVersionNumber = this.reverse.get(nbt);
        if (existingVersionNumber != null) {
            System.out.println("Reusing nbt part");
            return existingVersionNumber;
        }
        int versionNumber = 1;
        while (this.data.containsKey(versionNumber)) {
            versionNumber++;
        }
        System.out.println("Storing new nbt part " + versionNumber);
        this.set(versionNumber, nbt);
        return versionNumber;
    }

    /**
     * Get an nbt tag from the storage by the tag's version number.
     *
     * @param versionNumber The version number.
     * @return The tag.
     * @throws RuntimeException If the nbt tag could not be found.
     */
    @NotNull
    public NbtCompound get(int versionNumber) {
        NbtCompound nbt = this.data.get(versionNumber);
        if (nbt == null) {
            throw new RuntimeException("Unable to find nbt with version number " + versionNumber + " in the nbt storage.");
        }
        return nbt;
    }

    @Override
    public String toString() {
        return "NbtPartStorage" + this.data;
    }
}
