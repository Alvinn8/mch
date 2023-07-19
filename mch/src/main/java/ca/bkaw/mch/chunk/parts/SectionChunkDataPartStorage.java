package ca.bkaw.mch.chunk.parts;

import ca.bkaw.mch.NbtStorage;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtList;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.util.BiMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A chunk data part that stores the chunk sections.
 * <p>
 * Each chunk section is stored separately so that when one chunk section changes,
 * only the changing chunk section needs to be saved again.
 */
public class SectionChunkDataPartStorage implements ChunkDataPartStorage {
    /**
     * The key of the sections tag.
     */
    public static final String SECTIONS_TAG = "sections";
    /**
     * The version number returned when there was no sections tag in the chunk.
     */
    public static final int NO_SECTIONS_TAG = 0;

    private final BiMap<Integer, MchSectionsList> sectionVersions;
    private final List<NbtStorage> sectionNbtStorage;

    public SectionChunkDataPartStorage(DataInput dataInput) throws IOException {
        int sectionVersionsSize = dataInput.readInt();
        this.sectionVersions = new BiMap<>(sectionVersionsSize);
        for (int i = 0; i < sectionVersionsSize; i++) {
            int sectionVersionNumber = dataInput.readInt();
            MchSectionsList mchSectionsList = new MchSectionsList(dataInput);
            this.sectionVersions.put(sectionVersionNumber, mchSectionsList);
        }
        int nbtStorageCount = dataInput.readInt();
        this.sectionNbtStorage = new ArrayList<>(nbtStorageCount);
        for (int i = 0; i < nbtStorageCount; i++) {
            this.sectionNbtStorage.add(new NbtStorage(dataInput));
        }
    }

    public SectionChunkDataPartStorage() {
        this.sectionVersions = new BiMap<>();
        this.sectionNbtStorage = new ArrayList<>(25);
        // 25 is the usual length of the list
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(this.sectionVersions.size());
        for (Map.Entry<Integer, MchSectionsList> entry : this.sectionVersions.entrySet()) {
            dataOutput.writeInt(entry.getKey());
            entry.getValue().write(dataOutput);
        }
        dataOutput.writeInt(this.sectionNbtStorage.size());
        for (NbtStorage nbtStorage : this.sectionNbtStorage) {
            nbtStorage.write(dataOutput);
        }
    }

    @Override
    public int storePart(NbtCompound chunk) {
        NbtList sections = (NbtList) chunk.get(SECTIONS_TAG);
        if (sections == null) {
            return NO_SECTIONS_TAG;
        }
        chunk.remove(SECTIONS_TAG);

        NbtTag[] sectionsArray = sections.getValue();
        MchSectionsList mchSectionsList = new MchSectionsList(sectionsArray.length);

        // Ensure we have enough nbt storages to store all sections
        if (this.sectionNbtStorage.size() < sectionsArray.length) {
            for (int i = this.sectionNbtStorage.size(); i < sectionsArray.length; i++) {
                this.sectionNbtStorage.add(new NbtStorage());
            }
        }

        // Store each section
        for (int sectionIndex = 0; sectionIndex < sectionsArray.length; sectionIndex++) {
            NbtCompound section = (NbtCompound) sectionsArray[sectionIndex];

            NbtStorage nbtStorage = this.sectionNbtStorage.get(sectionIndex);
            int sectionVersionNumber = nbtStorage.store(section);

            mchSectionsList.setSectionVersionNumber(sectionIndex, sectionVersionNumber);
        }

        // Check if the newly created MchSectionsList instance is equal to an already
        // existing list in the storage. In that case we reuse the existing list instead
        // of storing it again and return the version number of the existing list.
        Integer existingVersionNumber = this.sectionVersions.reverse().get(mchSectionsList);
        if (existingVersionNumber != null) {
            return existingVersionNumber;
        }

        // Find an unoccupied version number.
        int versionNumber = 1;
        while (this.sectionVersions.containsKey(versionNumber)) {
            versionNumber++;
        }

        this.sectionVersions.put(versionNumber, mchSectionsList);

        return versionNumber;
    }

    @Override
    public void restorePart(NbtCompound chunk, int versionNumber) {
        if (versionNumber == NO_SECTIONS_TAG) {
            // No sections tag was present, there is nothing to add to the chunk nbt.
            return;
        }

        MchSectionsList mchSectionsList = this.sectionVersions.get(versionNumber);
        if (mchSectionsList == null) {
            throw new RuntimeException("Sections list with version number " + versionNumber + " was not present in storage.");
        }

        int[] sectionVersionNumbers = mchSectionsList.sectionVersionNumbers;
        NbtList sectionsNbt = new NbtList(NbtCompound.ID, sectionVersionNumbers.length);
        for (int sectionIndex = 0; sectionIndex < sectionVersionNumbers.length; sectionIndex++) {
            int sectionVersionNumber = sectionVersionNumbers[sectionIndex];
            NbtStorage nbtStorage = this.sectionNbtStorage.get(sectionIndex);
            NbtCompound sectionNbt = nbtStorage.get(sectionVersionNumber);
            sectionsNbt.getValue()[sectionIndex] = sectionNbt;
        }

        chunk.set(SECTIONS_TAG, sectionsNbt);
    }

    public static class MchSectionsList {
        private final int[] sectionVersionNumbers;

        public MchSectionsList(DataInput dataInput) throws IOException {
            int size = dataInput.readInt();
            this.sectionVersionNumbers = new int[size];
            for (int i = 0; i < size; i++) {
                this.sectionVersionNumbers[i] = dataInput.readInt();
            }
        }

        public MchSectionsList(int size) {
            this.sectionVersionNumbers = new int[size];
        }

        public void write(DataOutput dataOutput) throws IOException {
            dataOutput.writeInt(this.sectionVersionNumbers.length);
            for (int sectionVersionNumber : this.sectionVersionNumbers) {
                dataOutput.writeInt(sectionVersionNumber);
            }
        }

        public void setSectionVersionNumber(int sectionIndex, int versionNumber) {
            this.sectionVersionNumbers[sectionIndex] = versionNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MchSectionsList that = (MchSectionsList) o;

            return Arrays.equals(this.sectionVersionNumbers, that.sectionVersionNumbers);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.sectionVersionNumbers);
        }
    }
}
