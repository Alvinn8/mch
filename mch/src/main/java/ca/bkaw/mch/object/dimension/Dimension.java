package ca.bkaw.mch.object.dimension;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.object.tree.Tree;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Dimension extends StorageObject {
    public static final int MAGIC = FileMagic.DIMENSION;

    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    public static final String THE_END = "minecraft:the_end";

    private final Reference20<Tree> miscellaneousFiles;
    private final List<RegionFileReference> regionFiles;

    public Dimension(Reference20<Tree> miscellaneousFiles) {
        this.miscellaneousFiles = miscellaneousFiles;
        this.regionFiles = new ArrayList<>();
    }

    public Dimension(DataInput dataInput) throws IOException {
        FileMagic.validate(dataInput, MAGIC);
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 4);
        this.miscellaneousFiles = Reference20.read(dataInput, ObjectStorageTypes.TREE);
        int size = dataInput.readInt();
        this.regionFiles = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.regionFiles.add(new RegionFileReference(dataInput));
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MAGIC);
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        this.miscellaneousFiles.write(dataOutput);
        dataOutput.writeInt(this.regionFiles.size());
        for (RegionFileReference regionFile : this.regionFiles) {
            regionFile.write(dataOutput);
        }
    }

    @Override
    public String cat() {
        StringBuilder str = new StringBuilder("region files:\n");
        for (RegionFileReference regionFile : this.regionFiles) {
            str.append("region ");
            str.append(regionFile.regionX);
            str.append(" ");
            str.append(regionFile.regionZ);
            str.append(":\tversion number: ");
            str.append(regionFile.versionNumber);
            str.append("\n");
        }
        if (this.regionFiles.isEmpty()) {
            str.append("(empty)\n");
        }
        str.append("miscellaneous files: ");
        str.append(this.miscellaneousFiles.getSha1().asHex());
        return str.toString();
    }

    public void addRegionFile(RegionFileReference regionFileReference) {
        this.regionFiles.add(regionFileReference);
    }

    @Nullable
    public RegionFileReference getRegionFile(int regionX, int regionZ) {
        for (RegionFileReference regionFile : this.regionFiles) {
            if (regionFile.regionX == regionX && regionFile.regionZ == regionZ) {
                return regionFile;
            }
        }
        return null;
    }

    public static class RegionFileReference {
        private final int regionX;
        private final int regionZ;
        private final int versionNumber;
        private final long lastModifiedTime;

        public RegionFileReference(DataInput dataInput) throws IOException {
            this.regionX = dataInput.readInt();
            this.regionZ = dataInput.readInt();
            this.versionNumber = dataInput.readInt();
            this.lastModifiedTime = dataInput.readLong();
        }

        public RegionFileReference(int regionX, int regionZ, int versionNumber, long lastModifiedTime) {
            this.regionX = regionX;
            this.regionZ = regionZ;
            this.versionNumber = versionNumber;
            this.lastModifiedTime = lastModifiedTime;
        }

        public void write(DataOutput dataOutput) throws IOException {
            dataOutput.writeInt(this.regionX);
            dataOutput.writeInt(this.regionZ);
            dataOutput.writeInt(this.versionNumber);
            dataOutput.writeLong(this.lastModifiedTime);
        }

        public int getVersionNumber() {
            return this.versionNumber;
        }

        public long getLastModifiedTime() {
            return this.lastModifiedTime;
        }
    }
}
