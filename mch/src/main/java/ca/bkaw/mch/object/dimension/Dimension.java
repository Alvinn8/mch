package ca.bkaw.mch.object.dimension;

import ca.bkaw.mch.object.StorageObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Dimension extends StorageObject {
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    public static final String THE_END = "minecraft:the_end";

    private final List<RegionFileReference> regionFiles;

    public Dimension() {
        this.regionFiles = new ArrayList<>();
    }

    public Dimension(DataInput dataInput) throws IOException {
        int size = dataInput.readInt();
        this.regionFiles = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.regionFiles.add(new RegionFileReference(dataInput));
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
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
            str.append(": version number:");
            str.append(regionFile.versionNumber);
        }
        if (this.regionFiles.isEmpty()) {
            str.append("(empty)");
        }
        return str.toString();
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
    }
}
